package com.eia.directclaims.domain;

import java.time.LocalDate;

/**
 * Raised when an allocation's policy year falls outside the coverage block of
 * a COMPLETE Policy mass-tort profile. The public API renders it as a 400 with
 * code {@code MASS_TORT_YEAR_OUTSIDE_BLOCK}; the block bounds are inclusive.
 */
public class MassTortYearOutsideBlockException extends RuntimeException {
    private final int policyYear;
    private final LocalDate coverageBlockStart;
    private final LocalDate coverageBlockEnd;

    public MassTortYearOutsideBlockException(int policyYear,
                                             LocalDate coverageBlockStart,
                                             LocalDate coverageBlockEnd) {
        super("policyYear " + policyYear + " is outside the mass tort coverage block "
                + coverageBlockStart + " to " + coverageBlockEnd);
        this.policyYear = policyYear;
        this.coverageBlockStart = coverageBlockStart;
        this.coverageBlockEnd = coverageBlockEnd;
    }

    public int policyYear() {
        return policyYear;
    }

    public LocalDate coverageBlockStart() {
        return coverageBlockStart;
    }

    public LocalDate coverageBlockEnd() {
        return coverageBlockEnd;
    }
}
