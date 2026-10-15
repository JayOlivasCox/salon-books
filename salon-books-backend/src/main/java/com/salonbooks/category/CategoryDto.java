package com.salonbooks.category;

import com.salonbooks.common.Require;
import com.salonbooks.common.SyncDto;
import com.salonbooks.tax.ScheduleCLine;

import java.time.Instant;
import java.util.UUID;

public record CategoryDto(UUID id, Instant clientModifiedAt, Boolean deleted,
                          String name, ScheduleCLine scheduleCLine, String color, Integer sortOrder)
    implements SyncDto<ExpenseCategory> {

    @Override
    public void applyTo(ExpenseCategory e) {
        e.setName(Require.notBlank(name, "name"));
        e.setScheduleCLine(Require.notNull(scheduleCLine, "scheduleCLine"));
        e.setColor(Require.trimToNull(color));
        if (sortOrder != null) e.setSortOrder(sortOrder);
    }
}
