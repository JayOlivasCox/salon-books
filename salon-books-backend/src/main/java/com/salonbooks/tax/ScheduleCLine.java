package com.salonbooks.tax;

/** IRS Schedule C (Form 1040) expense lines relevant to a booth-rent stylist. */
public enum ScheduleCLine {
    ADVERTISING("8", "Advertising"),
    CAR_TRUCK("9", "Car and truck expenses"),
    COMMISSIONS_FEES("10", "Commissions and fees"),
    CONTRACT_LABOR("11", "Contract labor"),
    DEPRECIATION("13", "Depreciation and section 179 expense"),
    INSURANCE("15", "Insurance (other than health)"),
    INTEREST_OTHER("16b", "Interest (other)"),
    LEGAL_PROFESSIONAL("17", "Legal and professional services"),
    OFFICE_EXPENSE("18", "Office expense"),
    RENT_EQUIPMENT("20a", "Rent or lease: vehicles, machinery, and equipment"),
    RENT_OTHER_PROPERTY("20b", "Rent or lease: other business property"),
    REPAIRS("21", "Repairs and maintenance"),
    SUPPLIES("22", "Supplies"),
    TAXES_LICENSES("23", "Taxes and licenses"),
    TRAVEL("24a", "Travel"),
    MEALS("24b", "Deductible meals"),
    UTILITIES("25", "Utilities"),
    WAGES("26", "Wages"),
    OTHER("27a", "Other expenses"),
    COGS("Part III", "Cost of goods sold (retail products for resale)");

    private final String line;
    private final String label;

    ScheduleCLine(String line, String label) {
        this.line = line;
        this.label = label;
    }

    public String line() {
        return line;
    }

    public String label() {
        return label;
    }
}
