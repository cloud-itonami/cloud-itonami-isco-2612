# cloud-itonami-isco-2612

Open Occupation Blueprint for **ISCO-08 2612**: Judges.

This repository designs a forkable OSS business for an independent arbitration and adjudication practice: a hearing-record and exhibit-handling robot manages case materials under a governor-gated actor, so the practice keeps its own adjudication records instead of renting a closed case-management SaaS. This practice offers private arbitration, mediation and adjudication services, not public judicial office.

**Maturity: `:implemented`.** `src/arbitration/` implements the
`ArbitrationActor` as a `langgraph.graph/state-graph`
(`arbitration.actor`) wired to an `Arbitration Advisor` (`arbitration.advisor`)
and an independent `ArbitrationGovernor` (`arbitration.governor`),
following the itonami actor pattern (ADR-2607011000): `:intake -> :advise
-> :govern -> :decide -+-> :commit (:ok?) +-> :request-approval (:escalate?,
human-in-the-loop interrupt) +-> :hold (:hard?)`. 14 tests / 29 assertions
green (`kbb -M:test`). HARD invariants (always hold, never
overridable): client provenance, no-actuation (`:effect` must be
`:propose`), a registered case basis for any finding proposal, the
proposed award amount not exceeding the case's registered
jurisdictional/agreed award ceiling (awarding beyond the case's
registered ceiling is an ultra vires ruling, not a generous award), and
a cleared recusal/conflict-of-interest check before any finding can be
drafted (drafting a finding without a cleared recusal check is a
conflict-of-interest violation, not efficient service). Always-escalate
ops (human sign-off regardless of confidence, mapping this repo's Trust
Controls in [`docs/business-model.md`](docs/business-model.md)):
`:approve-binding-award-issuance` (no binding award issuance without the
governor gate) and `:approve-case-acceptance` (accepting a case always
requires human sign-off).

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a hearing-record and exhibit-handling robot performs exhibit intake, transcript binding and hearing-room material organization under an actor that proposes
actions and an independent **Arbitration Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
binding award issuance) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
case submission + party consent + hearing record
        |
        v
Arbitration Advisor -> Arbitration Governor -> draft finding, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `2612`). Required capabilities:

- :robotics
- :identity
- :forms
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
