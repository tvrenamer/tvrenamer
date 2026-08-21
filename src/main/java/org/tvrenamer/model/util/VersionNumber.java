package org.tvrenamer.model.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orders TVRenamer version numbers.
 *
 * <p>A version is a dot-separated numeric part, optionally followed by a beta
 * number: {@code 0.8}, {@code 0.7.2}, {@code 1.0b5}. Releases up to 0.6 spelled
 * the beta out as {@code 0.6-beta-1}, so that form is accepted too.
 *
 * <p>Ordering a version is not the same as ordering its text. A beta leads up to
 * a release, so it has to sort <em>before</em> it: {@code 1.0b5} is older than
 * {@code 1.0}, where comparing the strings says the opposite. Beta numbers and
 * release components are compared as numbers, not digits, so {@code 1.0b10} is
 * newer than {@code 1.0b9} and {@code 0.10} is newer than {@code 0.9}.
 *
 * <p>Trailing zeroes do not change a version, so {@code 1.0} and {@code 1.0.0}
 * are equal.
 */
public final class VersionNumber implements Comparable<VersionNumber> {

    private static final Pattern VERSION
        = Pattern.compile("(\\d+(?:\\.\\d+)*)(?:(?:-beta-|b)(\\d+))?");

    // A release is the newest thing carrying its number, so it has to sort after
    // every beta that led up to it.
    private static final int NOT_A_BETA = Integer.MAX_VALUE;

    private final int[] components;
    private final int beta;

    private VersionNumber(final int[] components, final int beta) {
        this.components = components;
        this.beta = beta;
    }

    /**
     * Reads a version number.
     *
     * @param text
     *   the version as it appears in the version file or on the server
     * @return
     *   the parsed version, or null if the text is not a version number we
     *   recognise; callers should treat null as "cannot tell", not as "older"
     */
    public static VersionNumber parse(final String text) {
        if (text == null) {
            return null;
        }
        final Matcher matcher = VERSION.matcher(text.trim().toLowerCase());
        if (!matcher.matches()) {
            return null;
        }

        final String[] parts = matcher.group(1).split("\\.");
        final int[] components = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                components[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                // The pattern only admits digits, so this means a component too
                // large to hold. Refusing to guess is better than wrapping.
                return null;
            }
        }

        int beta = NOT_A_BETA;
        if (matcher.group(2) != null) {
            try {
                beta = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new VersionNumber(components, beta);
    }

    /**
     * Says whether one version is newer than another.
     *
     * @param candidate
     *   the version being offered, as text
     * @param current
     *   the version in hand, as text
     * @return
     *   true only if both parse and the candidate is strictly newer; an
     *   unreadable version on either side gives false, so a garbled answer from
     *   the server cannot trigger a spurious update prompt
     */
    public static boolean isNewer(final String candidate, final String current) {
        final VersionNumber offered = parse(candidate);
        final VersionNumber running = parse(current);
        if ((offered == null) || (running == null)) {
            return false;
        }
        return offered.compareTo(running) > 0;
    }

    @Override
    public int compareTo(final VersionNumber other) {
        final int longest = Math.max(components.length, other.components.length);
        for (int i = 0; i < longest; i++) {
            // A version that runs out of components reads as zero from there on,
            // which is what makes 1.0 and 1.0.0 the same version.
            final int mine = (i < components.length) ? components[i] : 0;
            final int theirs = (i < other.components.length) ? other.components[i] : 0;
            if (mine != theirs) {
                return (mine < theirs) ? -1 : 1;
            }
        }
        return Integer.compare(beta, other.beta);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VersionNumber)) {
            return false;
        }
        return compareTo((VersionNumber) other) == 0;
    }

    @Override
    public int hashCode() {
        // Must agree with equals, which ignores trailing zeroes, so the trailing
        // zeroes cannot be allowed to contribute.
        int last = components.length;
        while ((last > 0) && (components[last - 1] == 0)) {
            last--;
        }
        int hash = beta;
        for (int i = 0; i < last; i++) {
            hash = (31 * hash) + components[i];
        }
        return hash;
    }
}
