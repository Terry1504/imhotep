package com.indeed.imhotep.exceptions;

import com.indeed.imhotep.api.ImhotepOutOfMemoryException;
import com.indeed.imhotep.io.TempFileSizeLimitExceededException;
import com.indeed.imhotep.io.WriteLimitExceededException;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Tries to resolve Imhotep errors which had their types lost in Protobuf shipping to a more structured representation
 * @author vladimir
 */
public class ImhotepErrorResolver {
    private ImhotepErrorResolver() {
    }

    public static Exception resolve(final Exception e) {
        final String error = ExceptionUtils.getRootCauseMessage(e);
        if (error.contains(ImhotepOutOfMemoryException.class.getSimpleName())) {
            return new ImhotepOverloadedException("Imhotep is overloaded with all memory in use. " +
                    "Please wait before retrying.", e);
        }

        if (error.contains(TooManySessionsException.class.getSimpleName())) {
            return new ImhotepOverloadedException("Imhotep is overloaded with too many concurrent sessions. " +
                    "Please wait before retrying.", e);
        }

        if (error.contains(UserSessionCountLimitExceededException.class.getSimpleName())) {
            return new UserSessionCountLimitExceededException("You have too many concurrent Imhotep sessions running. " +
                    "Please wait for them to complete before retrying.", e);
        }

        if (error.contains(TempFileSizeLimitExceededException.class.getSimpleName()) ||
                error.contains(WriteLimitExceededException.class.getSimpleName())) {
            return new FTGSLimitExceededException("The query tried to iterate over too much data. " +
                    "Please simplify the query.", e);
        }

        if (error.contains(MultiValuedFieldRegroupException.class.getSimpleName())) {
            return new MultiValuedFieldRegroupException("Query failed trying to do an Imhotep regroup on a " +
                    "multi-valued field. Grouping by a multi-valued field only works if it's the last group by and percentile/distinct are not used.", e);
        }

        if (error.contains(RegexTooComplexException.class.getSimpleName())) {
            return new RegexTooComplexException("The provided regex is too complex. " +
                    "Please replace expressions like '.*A.*|.*B.*|.*C.*' with '.*(A|B|C).*'", e);
        }

        if (error.contains("BitSet fields should only have term")) {
            return new StringFieldInSelectException("The query attempted to use a string field where a field with " +
                    "only numeric values is required. For example only int fields and string fields containing only " +
                    "numbers can be used in the SELECT clause.", e);
        }

        if (error.contains("there does not exist a session with id")) {
            return new QueryCancelledException("The query was cancelled during execution", e);
        }

        return e;
    }
}
