Translations are managed via Crowdin: https://crowdin.com/project/distant-horizons

## How to help:
1. Create a Crowdin account 
2. Join the project
3. Translate strings from `en_us.json`. 

## Notes:
- Keys ending with `@tooltip` are tooltips.
- Keep formatting codes intact, IE: `§`, `%s`, `%d`, `%1$s`.
- For newlines, utilize **Shift + Enter** instead of `\n`.
- Do not edit non-English files in pull requests.

## To pull translations into the repo
- Downlod the [Crowdin CLI](https://github.com/crowdin/crowdin-cli/releases)
- Run `crowdin download --export-only-approved --skip-untranslated-files` in the project root.