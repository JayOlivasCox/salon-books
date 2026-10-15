package com.salonbooks.report;

import com.salonbooks.appointment.Appointment;
import com.salonbooks.appointment.AppointmentRepository;
import com.salonbooks.appointment.AppointmentStatus;
import com.salonbooks.business.Business;
import com.salonbooks.business.RentDisplay;
import com.salonbooks.client.Client;
import com.salonbooks.client.ClientRepository;
import com.salonbooks.income.IncomeEntry;
import com.salonbooks.income.IncomeEntryRepository;
import com.salonbooks.lease.Lease;
import com.salonbooks.lease.LeaseService;
import com.salonbooks.services.ServiceType;
import com.salonbooks.services.ServiceTypeRepository;
import com.salonbooks.tax.ScheduleCLine;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.salonbooks.report.ReportModels.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private static final int TOP_VENDORS = 5;

    /**
     * Every expense as attributed lines: each item (with its own category if set), plus the
     * remainder of the total that items don't cover (tax, or the whole thing if not itemized).
     */
    static final String EXPENSE_LINES = """
        select e.expense_date, e.vendor, e.category_id,
               e.total - coalesce((select sum(i.amount) from expense_item i where i.expense_id = e.id), 0) as amount
          from expense e
         where e.business_id = :bid and e.deleted_at is null and e.expense_date between :from and :to
        union all
        select e.expense_date, e.vendor, coalesce(i.category_id, e.category_id), i.amount
          from expense e
          join expense_item i on i.expense_id = e.id
         where e.business_id = :bid and e.deleted_at is null and e.expense_date between :from and :to
        """;

    private final NamedParameterJdbcTemplate jdbc;
    private final LeaseService leases;
    private final AppointmentRepository appointments;
    private final IncomeEntryRepository incomeEntries;
    private final ClientRepository clients;
    private final ServiceTypeRepository serviceTypes;

    // ---------- building blocks ----------

    public IncomeTotals income(UUID bid, LocalDate from, LocalDate to) {
        return jdbc.queryForObject("""
            select coalesce(sum(service_amount), 0) as services, coalesce(sum(tip_amount), 0) as tips, count(*) as n
              from income_entry
             where business_id = :bid and deleted_at is null and service_date between :from and :to
            """, params(bid, from, to), (rs, i) -> {
            BigDecimal s = rs.getBigDecimal("services"), t = rs.getBigDecimal("tips");
            return new IncomeTotals(s, t, s.add(t), rs.getLong("n"));
        });
    }

    public List<CategoryTotal> byCategory(UUID bid, LocalDate from, LocalDate to) {
        return jdbc.query("""
            select l.category_id, c.name, c.color, c.schedule_c_line, sum(l.amount) as amount
              from (%s) l
              left join expense_category c on c.id = l.category_id
             group by l.category_id, c.name, c.color, c.schedule_c_line
            having sum(l.amount) <> 0
             order by amount desc
            """.formatted(EXPENSE_LINES), params(bid, from, to), (rs, i) -> new CategoryTotal(
            rs.getObject("category_id", UUID.class),
            Objects.requireNonNullElse(rs.getString("name"), "Uncategorized"),
            rs.getString("color"),
            line(rs.getString("schedule_c_line")),
            rs.getBigDecimal("amount")));
    }

    public List<ExpenseLine> expenseLines(UUID bid, LocalDate from, LocalDate to) {
        return jdbc.query("""
            select l.expense_date, l.vendor, l.category_id, c.name, c.schedule_c_line, l.amount
              from (%s) l
              left join expense_category c on c.id = l.category_id
             where l.amount <> 0
             order by l.expense_date, l.vendor
            """.formatted(EXPENSE_LINES), params(bid, from, to), (rs, i) -> new ExpenseLine(
            rs.getObject("expense_date", LocalDate.class),
            rs.getString("vendor"),
            rs.getObject("category_id", UUID.class),
            Objects.requireNonNullElse(rs.getString("name"), "Uncategorized"),
            line(rs.getString("schedule_c_line")),
            rs.getBigDecimal("amount")));
    }

    public List<VendorTotal> topVendors(UUID bid, LocalDate from, LocalDate to, int limit) {
        return jdbc.query("""
            select max(trim(vendor)) as vendor, sum(total) as amount, count(*) as n
              from expense
             where business_id = :bid and deleted_at is null and expense_date between :from and :to
               and vendor is not null and trim(vendor) <> ''
             group by lower(trim(vendor))
             order by amount desc
             limit :limit
            """, params(bid, from, to).addValue("limit", limit), (rs, i) -> new VendorTotal(
            rs.getString("vendor"), rs.getBigDecimal("amount"), rs.getLong("n")));
    }

    public PeriodSummary summary(UUID bid, LocalDate from, LocalDate to) {
        IncomeTotals inc = income(bid, from, to);
        List<CategoryTotal> cats = byCategory(bid, from, to);
        BigDecimal expenses = cats.stream().map(CategoryTotal::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rent = leases.rentForRange(bid, from, to);
        BigDecimal totalExp = expenses.add(rent);
        return new PeriodSummary(from, to, inc, expenses, rent, totalExp, inc.gross().subtract(totalExp),
            cats, topVendors(bid, from, to, TOP_VENDORS));
    }

    // ---------- screens ----------

    public DailyReport daily(Business b, LocalDate date, UUID stylistId) {
        ZoneId zone = b.zone();
        var dayAppts = appointments
            .findByBusinessIdAndDeletedAtIsNullAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                b.getId(), date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
            .stream()
            .filter(a -> stylistId == null || stylistId.equals(a.getStylistId()))
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .toList();
        List<IncomeEntry> entries = incomeEntries
            .findByBusinessIdAndDeletedAtIsNullAndServiceDateBetweenOrderByServiceDateAscCreatedAtAsc(b.getId(), date, date)
            .stream()
            .filter(e -> stylistId == null || stylistId.equals(e.getStylistId()))
            .toList();

        Map<UUID, String> clientNames = clients.findAllById(dayAppts.stream().map(Appointment::getClientId)
                .filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Client::getId, Client::getName));
        Map<UUID, String> serviceNames = serviceTypes.findAllById(dayAppts.stream().map(Appointment::getServiceTypeId)
                .filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(ServiceType::getId, ServiceType::getName));
        Map<UUID, UUID> incomeByAppt = new HashMap<>();
        for (IncomeEntry e : entries) if (e.getAppointmentId() != null) incomeByAppt.put(e.getAppointmentId(), e.getId());

        int unnamed = 0;
        List<AppointmentView> views = new ArrayList<>();
        for (Appointment a : dayAppts) {
            String name = a.getClientId() != null ? clientNames.get(a.getClientId()) : a.getClientLabel();
            if (name == null) unnamed++;
            views.add(new AppointmentView(a.getId(), a.getStartsAt(), a.getEndsAt(), a.getStylistId(), a.getClientId(),
                name, a.getServiceTypeId(), serviceNames.get(a.getServiceTypeId()), a.getStatus(), incomeByAppt.get(a.getId())));
        }

        BigDecimal svc = entries.stream().map(IncomeEntry::getServiceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tips = entries.stream().map(IncomeEntry::getTipAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DailyReport(date, views, views.size(), unnamed, entries,
            new IncomeTotals(svc, tips, svc.add(tips), entries.size()));
    }

    public WeeklyReport weekly(Business b, LocalDate weekStart) {
        LocalDate end = weekStart.plusDays(6);
        PeriodSummary s = summary(b.getId(), weekStart, end);
        Map<LocalDate, DayTotal> byDay = jdbc.query("""
            select service_date, sum(service_amount) as s, sum(tip_amount) as t
              from income_entry
             where business_id = :bid and deleted_at is null and service_date between :from and :to
             group by service_date
            """, params(b.getId(), weekStart, end), (rs, i) -> {
            BigDecimal sv = rs.getBigDecimal("s"), t = rs.getBigDecimal("t");
            return new DayTotal(rs.getObject("service_date", LocalDate.class), sv, t, sv.add(t));
        }).stream().collect(Collectors.toMap(DayTotal::date, Function.identity()));
        List<DayTotal> days = new ArrayList<>();
        for (LocalDate d = weekStart; !d.isAfter(end); d = d.plusDays(1)) {
            days.add(byDay.getOrDefault(d, new DayTotal(d, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        }
        return new WeeklyReport(weekStart, s, b.getRentDisplay() == RentDisplay.WEEKLY, days);
    }

    public MonthlyReport monthly(Business b, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        PeriodSummary s = summary(b.getId(), ym.atDay(1), ym.atEndOfMonth());
        return new MonthlyReport(year, month, s, b.getRentDisplay() == RentDisplay.MONTHLY,
            compare(b.getId(), s, ym.minusMonths(1)), compare(b.getId(), s, ym.minusYears(1)));
    }

    public YearlyReport yearly(Business b, int year) {
        UUID bid = b.getId();
        LocalDate from = LocalDate.of(year, 1, 1), to = LocalDate.of(year, 12, 31);
        PeriodSummary s = summary(bid, from, to);

        MonthSeries cur = monthSeries(bid, year);
        MonthSeries prev = monthSeries(bid, year - 1);

        List<MonthRow> rows = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal gross = cur.services[m - 1].add(cur.tips[m - 1]);
            BigDecimal exp = cur.expenses[m - 1].add(cur.rent[m - 1]);
            rows.add(new MonthRow(m, cur.services[m - 1], cur.tips[m - 1], gross, cur.expenses[m - 1],
                cur.rent[m - 1], gross.subtract(exp)));
        }

        LocalDate today = LocalDate.now(b.zone());
        int completed = year < today.getYear() ? 12 : year == today.getYear() ? today.getMonthValue() - 1 : 0;

        List<MonthRow> finished = rows.subList(0, completed).stream()
            .filter(r -> r.gross().signum() > 0).toList();
        MonthStat busiest = finished.stream().max(Comparator.comparing(MonthRow::gross))
            .map(r -> new MonthStat(r.month(), r.gross())).orElse(null);
        MonthStat slowest = finished.stream().min(Comparator.comparing(MonthRow::gross))
            .map(r -> new MonthStat(r.month(), r.gross())).orElse(null);

        Projection.Result pg = Projection.nextYear(cur.gross(), prev.gross(), completed);
        Projection.Result pe = Projection.nextYear(cur.totalExpenses(), prev.totalExpenses(), completed);
        BigDecimal pGross = money(pg.total()), pExp = money(pe.total());
        ProjectionView projection = new ProjectionView(pg.method(), round4(pg.growthRate()),
            toMoneyList(pg.months()), toMoneyList(pe.months()), pGross, pExp, pGross.subtract(pExp));

        return new YearlyReport(year, s, rows, busiest, slowest, projection);
    }

    // ---------- helpers ----------

    private Comparison compare(UUID bid, PeriodSummary current, YearMonth other) {
        LocalDate from = other.atDay(1), to = other.atEndOfMonth();
        IncomeTotals inc = income(bid, from, to);
        BigDecimal exp = byCategory(bid, from, to).stream().map(CategoryTotal::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(leases.rentForRange(bid, from, to));
        BigDecimal net = inc.gross().subtract(exp);
        return new Comparison(from, to, inc.gross(), exp, net,
            pctChange(current.income().gross(), inc.gross()), pctChange(current.net(), net));
    }

    private record MonthSeries(BigDecimal[] services, BigDecimal[] tips, BigDecimal[] expenses, BigDecimal[] rent) {
        double[] gross() {
            double[] d = new double[12];
            for (int i = 0; i < 12; i++) d[i] = services[i].add(tips[i]).doubleValue();
            return d;
        }

        double[] totalExpenses() {
            double[] d = new double[12];
            for (int i = 0; i < 12; i++) d[i] = expenses[i].add(rent[i]).doubleValue();
            return d;
        }
    }

    private MonthSeries monthSeries(UUID bid, int year) {
        LocalDate from = LocalDate.of(year, 1, 1), to = LocalDate.of(year, 12, 31);
        BigDecimal[] svc = zeros(), tips = zeros(), exp = zeros(), rent = zeros();
        jdbc.query("""
            select extract(month from service_date)::int as m, sum(service_amount) as s, sum(tip_amount) as t
              from income_entry
             where business_id = :bid and deleted_at is null and service_date between :from and :to
             group by 1
            """, params(bid, from, to), rs -> {
            svc[rs.getInt("m") - 1] = rs.getBigDecimal("s");
            tips[rs.getInt("m") - 1] = rs.getBigDecimal("t");
        });
        jdbc.query("""
            select extract(month from expense_date)::int as m, sum(total) as x
              from expense
             where business_id = :bid and deleted_at is null and expense_date between :from and :to
             group by 1
            """, params(bid, from, to), rs -> {
            exp[rs.getInt("m") - 1] = rs.getBigDecimal("x");
        });
        List<Lease> yearLeases = leases.overlapping(bid, from, to);
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            rent[m - 1] = LeaseService.rentFor(yearLeases, ym.atDay(1), ym.atEndOfMonth());
        }
        return new MonthSeries(svc, tips, exp, rent);
    }

    private static MapSqlParameterSource params(UUID bid, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource().addValue("bid", bid).addValue("from", from).addValue("to", to);
    }

    private static ScheduleCLine line(String s) {
        if (s == null) return ScheduleCLine.OTHER;
        try {
            return ScheduleCLine.valueOf(s);
        } catch (IllegalArgumentException e) {
            return ScheduleCLine.OTHER;
        }
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] a = new BigDecimal[12];
        java.util.Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    private static Double pctChange(BigDecimal now, BigDecimal before) {
        if (before.signum() == 0) return null;
        return now.subtract(before).divide(before.abs(), 4, RoundingMode.HALF_UP).doubleValue() * 100;
    }

    private static BigDecimal money(double d) {
        return BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP);
    }

    private static double round4(double d) {
        return Math.round(d * 10000) / 10000.0;
    }

    private static List<BigDecimal> toMoneyList(double[] a) {
        List<BigDecimal> out = new ArrayList<>(a.length);
        for (double d : a) out.add(money(d));
        return out;
    }
}
