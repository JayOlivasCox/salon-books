package com.salonbooks.sync;

import com.salonbooks.appointment.Appointment;
import com.salonbooks.appointment.AppointmentDto;
import com.salonbooks.appointment.AppointmentRepository;
import com.salonbooks.category.CategoryDto;
import com.salonbooks.category.ExpenseCategory;
import com.salonbooks.category.ExpenseCategoryRepository;
import com.salonbooks.client.Client;
import com.salonbooks.client.ClientDto;
import com.salonbooks.client.ClientRepository;
import com.salonbooks.common.ApiException;
import com.salonbooks.common.SyncDto;
import com.salonbooks.common.SyncSupport;
import com.salonbooks.common.SyncableEntity;
import com.salonbooks.common.SyncableRepository;
import com.salonbooks.config.AppProperties;
import com.salonbooks.expense.Expense;
import com.salonbooks.expense.ExpenseDto;
import com.salonbooks.expense.ExpenseRepository;
import com.salonbooks.income.IncomeDto;
import com.salonbooks.income.IncomeEntry;
import com.salonbooks.income.IncomeEntryRepository;
import com.salonbooks.lease.Lease;
import com.salonbooks.lease.LeaseDto;
import com.salonbooks.lease.LeaseRepository;
import com.salonbooks.services.ServiceType;
import com.salonbooks.services.ServiceTypeDto;
import com.salonbooks.services.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Offline sync. The app keeps a local queue of changes and:
 *   1. POST /sync with everything queued (any order inside a type; types are applied parent-first)
 *   2. GET /sync?since=<nextSince from last pull> to get everyone's changes (including a partner's)
 * Pushing is idempotent (client UUIDs), so retrying after a dropped connection is safe.
 */
@Service
@RequiredArgsConstructor
public class SyncService {
    /** Pull windows overlap a little so a row committed during the previous pull isn't missed. */
    private static final long OVERLAP_SECONDS = 30;

    private final SyncSupport sync;
    private final AppProperties app;
    private final ExpenseCategoryRepository categories;
    private final ClientRepository clients;
    private final ServiceTypeRepository serviceTypes;
    private final LeaseRepository leases;
    private final AppointmentRepository appointments;
    private final IncomeEntryRepository income;
    private final ExpenseRepository expenses;

    public record PullResponse(Instant nextSince, List<ExpenseCategory> categories, List<Client> clients,
                               List<ServiceType> serviceTypes, List<Lease> leases, List<Appointment> appointments,
                               List<IncomeEntry> incomeEntries, List<Expense> expenses) {
    }

    public record PushRequest(List<CategoryDto> categories, List<ClientDto> clients, List<ServiceTypeDto> serviceTypes,
                              List<LeaseDto> leases, List<AppointmentDto> appointments, List<IncomeDto> incomeEntries,
                              List<ExpenseDto> expenses) {
    }

    public record ItemResult(String type, UUID id, SyncSupport.Status status, String error) {
    }

    public record PushResponse(int applied, int stale, int errors, List<ItemResult> results) {
    }

    @Transactional(readOnly = true)
    public PullResponse pull(UUID bid, Instant since) {
        Instant start = Instant.now();
        Instant s = since == null ? Instant.EPOCH : since;
        return new PullResponse(start.minusSeconds(OVERLAP_SECONDS),
            categories.findByBusinessIdAndUpdatedAtAfter(bid, s),
            clients.findByBusinessIdAndUpdatedAtAfter(bid, s),
            serviceTypes.findByBusinessIdAndUpdatedAtAfter(bid, s),
            leases.findByBusinessIdAndUpdatedAtAfter(bid, s),
            appointments.findByBusinessIdAndUpdatedAtAfter(bid, s),
            income.findByBusinessIdAndUpdatedAtAfter(bid, s),
            expenses.findByBusinessIdAndUpdatedAtAfter(bid, s));
    }

    /** Not transactional on purpose: each record commits on its own so one bad row doesn't sink the batch. */
    public PushResponse push(UUID bid, PushRequest req) {
        long count = Stream.of(req.categories(), req.clients(), req.serviceTypes(), req.leases(),
                req.appointments(), req.incomeEntries(), req.expenses())
            .mapToLong(l -> l == null ? 0 : l.size()).sum();
        if (count > app.maxSyncBatch()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Send at most " + app.maxSyncBatch() + " records per push");
        }

        List<ItemResult> out = new ArrayList<>();
        // Parents before children so foreign keys resolve.
        apply("category", req.categories(), categories, ExpenseCategory::new, bid, out);
        apply("client", req.clients(), clients, Client::new, bid, out);
        apply("serviceType", req.serviceTypes(), serviceTypes, ServiceType::new, bid, out);
        apply("lease", req.leases(), leases, Lease::new, bid, out);
        apply("appointment", req.appointments(), appointments, Appointment::new, bid, out);
        apply("incomeEntry", req.incomeEntries(), income, IncomeEntry::new, bid, out);
        apply("expense", req.expenses(), expenses, Expense::new, bid, out);

        int applied = 0, stale = 0, errors = 0;
        for (ItemResult r : out) {
            switch (r.status()) {
                case APPLIED, NOOP -> applied++;
                case STALE -> stale++;
                case ERROR -> errors++;
            }
        }
        return new PushResponse(applied, stale, errors, out);
    }

    private <E extends SyncableEntity, D extends SyncDto<E>> void apply(String type, List<D> items,
                                                                        SyncableRepository<E> repo, Supplier<E> factory,
                                                                        UUID bid, List<ItemResult> out) {
        if (items == null) return;
        for (D dto : items) {
            try {
                SyncSupport.Outcome<E> o = sync.upsert(repo, bid, dto, factory);
                out.add(new ItemResult(type, o.id(), o.status(), null));
            } catch (ApiException e) {
                out.add(new ItemResult(type, dto.id(), SyncSupport.Status.ERROR, e.getMessage()));
            } catch (DataIntegrityViolationException e) {
                out.add(new ItemResult(type, dto.id(), SyncSupport.Status.ERROR,
                    "Invalid reference or constraint violation"));
            } catch (RuntimeException e) {
                out.add(new ItemResult(type, dto.id(), SyncSupport.Status.ERROR, "Could not save record"));
            }
        }
    }
}
