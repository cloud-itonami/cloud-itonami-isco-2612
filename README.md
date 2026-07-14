# cloud-itonami-isco-2612

Open Occupation Blueprint for **ISCO-08 2612**: Judges.

This repository designs a forkable OSS business for an independent arbitration and adjudication practice: a hearing-record and exhibit-handling robot manages case materials under a governor-gated actor, so the practice keeps its own adjudication records instead of renting a closed case-management SaaS. This practice offers private arbitration, mediation and adjudication services, not public judicial office.

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
