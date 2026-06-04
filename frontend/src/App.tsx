import { FormEvent, ReactNode, useEffect, useMemo, useState } from "react";
import { createImpulseFilter, deleteImpulseFilter, getFilters } from "./api";
import type { AlertCreatedEvent, Direction, FilterResponse, ImpulseFilterRequest, StreamState } from "./types";

const exchangeOptions = ["BINANCE", "BYBIT"];
const marketOptions = ["FUTURES", "SPOT"];

const defaultForm: ImpulseFilterRequest = {
  exchange: ["BINANCE", "BYBIT"],
  market: ["FUTURES"],
  blackList: ["TON", "BNB"],
  action: "IMPULSE",
  timeWindow: 15,
  direction: "UP",
  percent: 3,
  volume24h: 100,
};

export function App() {
  const [userId, setUserId] = useState(1);
  const [filters, setFilters] = useState<FilterResponse[]>([]);
  const [alerts, setAlerts] = useState<AlertCreatedEvent[]>([]);
  const [form, setForm] = useState<ImpulseFilterRequest>(defaultForm);
  const [streamState, setStreamState] = useState<StreamState>("idle");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const lastAlert = alerts[0];
  const isValidUser = Number.isFinite(userId) && userId > 0;

  useEffect(() => {
    if (!isValidUser) return;

    setIsLoading(true);
    setError(null);

    getFilters(userId)
      .then(setFilters)
      .catch((e: Error) => setError(e.message))
      .finally(() => setIsLoading(false));
  }, [userId, isValidUser]);

  useEffect(() => {
    if (!isValidUser) return;

    setStreamState("connecting");
    const source = new EventSource(`/api/alerts/stream?userId=${userId}`);

    source.onopen = () => setStreamState("connected");
    source.onerror = () => setStreamState("error");
    source.onmessage = (event) => {
      if (event.data === ":connected") {
        setStreamState("connected");
        return;
      }

      try {
        const alert = JSON.parse(event.data) as AlertCreatedEvent;
        setAlerts((current) => [alert, ...current].slice(0, 80));
      } catch {
        setError("Failed to parse alert event");
      }
    };

    return () => {
      source.close();
      setStreamState("idle");
    };
  }, [userId, isValidUser]);

  const activeSymbols = useMemo(() => {
    return Array.from(new Set(alerts.slice(0, 20).map((alert) => alert.symbol))).slice(0, 6);
  }, [alerts]);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!isValidUser) return;

    setIsLoading(true);
    setError(null);

    try {
      const created = await createImpulseFilter(userId, form);
      setFilters((current) => [created, ...current.filter((filter) => filter.id !== created.id)]);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to create filter");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleDelete(filterId: number) {
    if (!isValidUser) return;

    setIsLoading(true);
    setError(null);

    try {
      await deleteImpulseFilter(userId, filterId);
      setFilters((current) => current.filter((filter) => filter.id !== filterId));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to delete filter");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <main className="app">
      <header className="topbar">
        <div>
          <p className="eyebrow">Market event screener</p>
          <h1>Alerting System</h1>
        </div>

        <label className="user-control">
          User ID
          <input
            type="number"
            min="1"
            value={userId}
            onChange={(event) => setUserId(Number(event.target.value))}
          />
        </label>
      </header>

      <section className="stats-grid">
        <Metric title="Stream" value={streamState} tone={streamState === "connected" ? "good" : "warn"} />
        <Metric title="Alerts" value={alerts.length.toString()} />
        <Metric title="Filters" value={filters.length.toString()} />
        <Metric title="Last symbol" value={lastAlert?.symbol ?? "none"} />
      </section>

      {error && <div className="error-line">{error}</div>}

      <section className="workspace">
        <form className="panel form-panel" onSubmit={handleCreate}>
          <div className="panel-head">
            <h2>Impulse filter</h2>
            <button disabled={isLoading || !isValidUser} type="submit">
              Create
            </button>
          </div>

          <FieldGroup title="Exchange">
            <CheckList
              options={exchangeOptions}
              values={form.exchange}
              onChange={(exchange) => setForm({ ...form, exchange })}
            />
          </FieldGroup>

          <FieldGroup title="Market">
            <CheckList
              options={marketOptions}
              values={form.market}
              onChange={(market) => setForm({ ...form, market })}
            />
          </FieldGroup>

          <div className="form-row">
            <label>
              Direction
              <select
                value={form.direction}
                onChange={(event) => setForm({ ...form, direction: event.target.value as Direction })}
              >
                <option value="UP">UP</option>
                <option value="DOWN">DOWN</option>
              </select>
            </label>

            <label>
              Percent
              <input
                min="1"
                type="number"
                value={form.percent}
                onChange={(event) => setForm({ ...form, percent: Number(event.target.value) })}
              />
            </label>
          </div>

          <div className="form-row">
            <label>
              Window, sec
              <input
                min="1"
                type="number"
                value={form.timeWindow}
                onChange={(event) => setForm({ ...form, timeWindow: Number(event.target.value) })}
              />
            </label>

            <label>
              Volume 24h
              <input
                min="0"
                type="number"
                value={form.volume24h}
                onChange={(event) => setForm({ ...form, volume24h: Number(event.target.value) })}
              />
            </label>
          </div>

          <label>
            Blacklist
            <input
              value={form.blackList.join(", ")}
              onChange={(event) =>
                setForm({
                  ...form,
                  blackList: event.target.value
                    .split(",")
                    .map((item) => item.trim().toUpperCase())
                    .filter(Boolean),
                })
              }
            />
          </label>
        </form>

        <section className="panel filters-panel">
          <div className="panel-head">
            <h2>Active filters</h2>
            <span>{isLoading ? "syncing" : "ready"}</span>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Direction</th>
                  <th>Percent</th>
                  <th>Window</th>
                  <th>Exchange</th>
                  <th>Market</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filters.map((filter) => (
                  <tr key={filter.id}>
                    <td>#{filter.id}</td>
                    <td className={filter.direction === "UP" ? "up" : "down"}>{filter.direction}</td>
                    <td>{filter.percent}%</td>
                    <td>{filter.timeWindow}s</td>
                    <td>{filter.exchange.join(", ")}</td>
                    <td>{filter.market.join(", ")}</td>
                    <td>
                      <button className="ghost" onClick={() => handleDelete(filter.id)} type="button">
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
                {filters.length === 0 && (
                  <tr>
                    <td className="empty" colSpan={7}>
                      No filters
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel alerts-panel">
          <div className="panel-head">
            <h2>Live alerts</h2>
            <div className="symbol-strip">
              {activeSymbols.map((symbol) => (
                <span key={symbol}>{symbol}</span>
              ))}
            </div>
          </div>

          <div className="alert-feed">
            {alerts.map((alert, index) => (
              <article className="alert-row" key={`${alert.filterId}-${alert.symbol}-${alert.timestampNs}-${index}`}>
                <div>
                  <strong>{alert.symbol}</strong>
                  <span>filter #{alert.filterId}</span>
                </div>
                <div>
                  <span>{alert.exchange.join(", ")}</span>
                  <span>{alert.market.join(", ")}</span>
                </div>
              </article>
            ))}
            {alerts.length === 0 && <div className="empty feed-empty">Waiting for alerts</div>}
          </div>
        </section>
      </section>
    </main>
  );
}

function Metric({ title, value, tone }: { title: string; value: string; tone?: "good" | "warn" }) {
  return (
    <div className="metric">
      <span>{title}</span>
      <strong className={tone}>{value}</strong>
    </div>
  );
}

function FieldGroup({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset>
      <legend>{title}</legend>
      {children}
    </fieldset>
  );
}

function CheckList({
  options,
  values,
  onChange,
}: {
  options: string[];
  values: string[];
  onChange: (next: string[]) => void;
}) {
  return (
    <div className="check-list">
      {options.map((option) => (
        <label key={option}>
          <input
            checked={values.includes(option)}
            type="checkbox"
            onChange={(event) => {
              if (event.target.checked) {
                onChange([...values, option]);
              } else {
                onChange(values.filter((value) => value !== option));
              }
            }}
          />
          {option}
        </label>
      ))}
    </div>
  );
}
