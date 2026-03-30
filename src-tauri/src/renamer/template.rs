// Rename format string evaluation
// Default template: "%S [%sx%0e] %t"  (show name, season, zero-padded episode, title)

/// Apply a rename template to episode metadata.
///
/// Tokens:
///   %S  — show name
///   %s  — season number (unpadded integer)
///   %0e — episode number, zero-padded to 2 digits
///   %t  — episode title
///
/// Unknown tokens are passed through unchanged.
pub fn apply_template(template: &str, show: &str, season: u32, episode: u32, title: &str) -> String {
    template
        .replace("%S", show)
        .replace("%s", &season.to_string())
        .replace("%0e", &format!("{:02}", episode))
        .replace("%t", title)
}

#[cfg(test)]
mod tests {
    use super::apply_template;

    #[test]
    fn default_template_single_digit_episode() {
        assert_eq!(
            apply_template("%S [%sx%0e] %t", "Breaking Bad", 1, 7, "A No-Rough-Stuff-Type Deal"),
            "Breaking Bad [1x07] A No-Rough-Stuff-Type Deal"
        );
    }

    #[test]
    fn default_template_double_digit_episode() {
        assert_eq!(
            apply_template("%S [%sx%0e] %t", "Breaking Bad", 2, 13, "Face Off"),
            "Breaking Bad [2x13] Face Off"
        );
    }

    #[test]
    fn episode_zero_padded_single_digit() {
        assert_eq!(apply_template("%0e", "Ignored", 0, 5, "Ignored"), "05");
    }

    #[test]
    fn episode_not_padded_when_two_digits() {
        assert_eq!(apply_template("%0e", "Ignored", 0, 15, "Ignored"), "15");
    }

    #[test]
    fn season_unpadded() {
        assert_eq!(apply_template("%s", "Ignored", 3, 0, "Ignored"), "3");
    }

    #[test]
    fn unknown_token_passes_through() {
        assert_eq!(apply_template("%X%Y", "Show", 1, 1, "Title"), "%X%Y");
    }

    #[test]
    fn empty_template_returns_empty() {
        assert_eq!(apply_template("", "Show", 1, 1, "Title"), "");
    }

    #[test]
    fn literal_text_preserved() {
        assert_eq!(apply_template("Episode %0e", "Ignored", 0, 3, "Ignored"), "Episode 03");
    }
}
