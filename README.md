# Bitcoin Custody Guardian

A fully-local, no-network Android app: private on-device AI that helps bitcoiners
(especially inheritors) understand and protect their self-custody setup.

- **Privacy is architectural:** the app never holds the INTERNET permission.
  Zero network code paths. Verified by monitoring tools: no traffic, ever.
- **The model never touches secrets:** seed phrases / xprvs pasted into the
  app are detected, warned about, and refused. The app never stores or
  transmits key material.
- **Grounded answers:** every substantive answer is drawn from a curated,
  versioned knowledge corpus and cites its source.

> **Status:** pre-code. The project is currently in specification phase.
> See [docs/](docs/). This repository is private during development and will
> be opened to the public before the first release.

## Documents
- [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) — product specification (v0.1)
- [docs/CORPUS_SOURCES.md](docs/CORPUS_SOURCES.md) — knowledge corpus sources & curation plan
- [docs/EVAL_SET.md](docs/EVAL_SET.md) — golden eval set structure & samples (release gate)

## License
MIT — see [LICENSE](LICENSE).
