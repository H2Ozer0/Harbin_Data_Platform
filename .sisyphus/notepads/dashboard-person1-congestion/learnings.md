# Learnings - ADS DDL Dashboard

## 2026-05-02: ADS DDL Execution

### Conventions
- SQL file follows `CREATE TABLE IF NOT EXISTS` pattern consistent with schema-perf.sql
- All tables in `ads` schema with explicit PRIMARY KEY constraints
- `DOUBLE PRECISION` for float columns, `INTEGER` for int, `DATE` for dates
- `TIMESTAMP DEFAULT NOW()` for audit columns
- `SERIAL PRIMARY KEY` for log/sequence tables

### Gotchas
- Splitting SQL on `;` fails if comment lines (`--`) are included — must strip comment lines before splitting
- PowerShell interferes with inline Python containing parentheses and quotes — use external .py script file

### Results
- 5 old ADS tables dropped successfully
- 7 new ADS tables created successfully
- `information_schema` confirms exactly 7 tables in ads schema
