package com.salonbooks.receipt;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReceiptParserTest {

    @Test
    void parsesTypicalSupplyStoreReceipt() {
        String text = """
            SALLY BEAUTY
            Store #1234
            123 Main St, Carson CA
            10/03/2026 2:14 PM
            WELLA COLOR CHARM 7N   8.99
            DEVELOPER 20V 32OZ    12.49 T
            FOILS 500CT           15.00
            SUBTOTAL              36.48
            TAX                    3.47
            TOTAL                 39.95
            VISA                  39.95
            CHANGE                 0.00
            """;
        ReceiptParser.Result r = ReceiptParser.parse(text);
        assertEquals("SALLY BEAUTY", r.vendor());
        assertEquals(LocalDate.of(2026, 10, 3), r.date());
        assertEquals(new BigDecimal("39.95"), r.total());
        assertEquals(new BigDecimal("36.48"), r.subtotal());
        assertEquals(new BigDecimal("3.47"), r.tax());
        assertEquals(3, r.items().size());
        assertEquals(new BigDecimal("12.49"), r.items().get(1).amount());
    }

    @Test
    void fallsBackToLargestAmountWithoutTotalLine() {
        ReceiptParser.Result r = ReceiptParser.parse("Corner Shop\nScissors $45.00\nComb $3.50\n");
        assertEquals(new BigDecimal("45.00"), r.total());
        assertEquals("Corner Shop", r.vendor());
    }

    @Test
    void handlesNamedDatesAndThousands() {
        ReceiptParser.Result r = ReceiptParser.parse("Pro Equipment Co\nOct 5, 2026\nStyling chair 1,250.00\nTotal 1,250.00");
        assertEquals(LocalDate.of(2026, 10, 5), r.date());
        assertEquals(new BigDecimal("1250.00"), r.total());
    }

    @Test
    void emptyTextIsSafe() {
        ReceiptParser.Result r = ReceiptParser.parse("");
        assertNull(r.total());
        assertTrue(r.items().isEmpty());
    }
}
