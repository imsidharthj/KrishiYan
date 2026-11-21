# Rules for working on KrishiYan

- Do not call a task done until it is fully complete and tested on a device.
- When you see a bug, fix it, even if it was there before your change.
- Work directly on `main`. There is one developer.
- Read `principles.md` and `docs/code-rules.md` before changing anything.
- Working documents (plans, specs) are temporary. When the work is done, move them to `old/`, which is gitignored. Never treat `old/` as current truth.
- Keep code simple. Avoid excessive comments. Never use em dashes in user-facing copy.

## Commits

- Plain lowercase messages, for example `add equipment list screen`.
- Stage files by name. Never use `git add .` or `git add -A`.
- Commit small and often, one change per commit.
