import { useEffect, useState } from "react";
import axios from "axios";
import Editor from "@monaco-editor/react";
import "./App.css";

const API_URL = "http://localhost:8080/api/shadow";

function App() {
  const [status, setStatus] = useState("UNKNOWN");
  const [message, setMessage] = useState("");

  const [employees, setEmployees] = useState([]);

  const [sql, setSql] = useState(
    "SELECT * FROM employees;"
  );

  const [queryResult, setQueryResult] = useState(null);
  const [queryError, setQueryError] = useState("");

  const [loading, setLoading] = useState(false);

  // =====================================
  // METRICS
  // =====================================

  const [metrics, setMetrics] = useState({
    queriesReplayed: 0,
    errors: 0,
    totalEvents: 0,
    errorRate: 0
  });

  const [metricsLoading, setMetricsLoading] =
    useState(false);

  // =====================================
  // RECENT CDC EVENTS
  // =====================================

  const [recentEvents, setRecentEvents] =
    useState([]);

  const [eventsLoading, setEventsLoading] =
    useState(false);

  // =====================================
  // TRAFFIC REPLAY
  // =====================================

  const [replayHistory, setReplayHistory] =
    useState([]);

  const [replayLoading, setReplayLoading] =
    useState(false);

  // =====================================
  // CHECK DATABASE STATUS
  // =====================================

  const checkStatus = async () => {
    try {
      const response = await axios.get(
        `${API_URL}/status`
      );

      const result = response.data;

      setMessage(result);

      if (result.includes("RUNNING")) {
        setStatus("RUNNING");
      } else {
        setStatus("STOPPED");
      }

    } catch (error) {
      console.error(error);

      setStatus("STOPPED");

      setMessage(
        "Unable to connect to the backend."
      );
    }
  };

  // =====================================
  // GET METRICS
  // =====================================

  const getMetrics = async () => {
    try {
      setMetricsLoading(true);

      const response = await axios.get(
        `${API_URL}/metrics`
      );

      setMetrics({
        queriesReplayed:
          response.data.queriesReplayed ?? 0,

        errors:
          response.data.errors ?? 0,

        totalEvents:
          response.data.totalEvents ?? 0,

        errorRate:
          response.data.errorRate ?? 0
      });

    } catch (error) {
      console.error(
        "Failed to load metrics:",
        error
      );

    } finally {
      setMetricsLoading(false);
    }
  };

  // =====================================
  // GET RECENT CDC EVENTS
  // =====================================

  const getRecentEvents = async () => {
    try {
      setEventsLoading(true);

      const response = await axios.get(
        `${API_URL}/metrics/events`
      );

      setRecentEvents(
        Array.isArray(response.data)
          ? response.data
          : []
      );

    } catch (error) {
      console.error(
        "Failed to load CDC events:",
        error
      );

    } finally {
      setEventsLoading(false);
    }
  };

  // =====================================
  // GET TRAFFIC REPLAY HISTORY
  // =====================================

  const getReplayHistory = async () => {
    try {
      setReplayLoading(true);

      const response = await axios.get(
        `${API_URL}/replay`
      );

      setReplayHistory(
        Array.isArray(response.data)
          ? response.data
          : []
      );

    } catch (error) {
      console.error(
        "Failed to load traffic replay history:",
        error
      );

    } finally {
      setReplayLoading(false);
    }
  };

  // =====================================
  // CLEAR TRAFFIC REPLAY HISTORY
  // =====================================

  const clearReplayHistory = async () => {
    try {
      setReplayLoading(true);

      await axios.delete(
        `${API_URL}/replay`
      );

      setReplayHistory([]);

      setMessage(
        "Traffic replay history cleared successfully."
      );

    } catch (error) {
      console.error(error);

      setMessage(
        error.response?.data ||
        "Failed to clear traffic replay history."
      );

    } finally {
      setReplayLoading(false);
    }
  };

  // =====================================
  // AUTOMATIC STATUS + LIVE REFRESH
  // =====================================

  useEffect(() => {
    checkStatus();

    getMetrics();

    getRecentEvents();

    getReplayHistory();

    const refreshInterval =
      setInterval(() => {
        getMetrics();
        getRecentEvents();
        getReplayHistory();
      }, 5000);

    return () => {
      clearInterval(refreshInterval);
    };

  }, []);

  // =====================================
  // CREATE DATABASE
  // =====================================

  const createDatabase = async () => {
    try {
      const response = await axios.post(
        `${API_URL}/create`
      );

      setMessage(response.data);

      setStatus("RUNNING");

      getMetrics();
      getRecentEvents();
      getReplayHistory();

    } catch (error) {
      console.error(error);

      setMessage(
        error.response?.data ||
        "Failed to create Shadow Database."
      );
    }
  };

  // =====================================
  // STOP DATABASE
  // =====================================

  const stopDatabase = async () => {
    try {
      const response = await axios.post(
        `${API_URL}/stop`
      );

      setMessage(response.data);

      setStatus("STOPPED");

      setQueryResult(null);

      setEmployees([]);

      setRecentEvents([]);

      setReplayHistory([]);

    } catch (error) {
      console.error(error);

      setMessage(
        error.response?.data ||
        "Failed to stop Shadow Database."
      );
    }
  };

  // =====================================
  // GET EMPLOYEES
  // =====================================

  const getEmployees = async () => {
    try {
      const response = await axios.get(
        `${API_URL}/employees`
      );

      setEmployees(response.data);

      setStatus("RUNNING");

      setMessage(
        "Employees loaded successfully."
      );

    } catch (error) {
      console.error(error);

      setMessage(
        error.response?.data ||
        "Failed to load employees."
      );
    }
  };

  // =====================================
  // EXECUTE SQL
  // =====================================

  const executeSql = async () => {
    if (!sql.trim()) {
      setQueryError(
        "Please enter a SQL query."
      );

      return;
    }

    setLoading(true);

    setQueryError("");

    setQueryResult(null);

    try {
      const response = await axios.post(
        `${API_URL}/sql`,
        {
          sql: sql
        }
      );

      setQueryResult(response.data);

      setStatus("RUNNING");

      setMessage(
        "SQL executed successfully."
      );

      getMetrics();
      getRecentEvents();
      getReplayHistory();

    } catch (error) {
      console.error(error);

      let errorMessage =
        "SQL execution failed.";

      if (error.response?.data) {
        if (
          typeof error.response.data === "string"
        ) {
          errorMessage =
            error.response.data;

        } else if (
          error.response.data.message
        ) {
          errorMessage =
            error.response.data.message;
        }
      }

      setQueryError(errorMessage);

      getMetrics();
      getRecentEvents();
      getReplayHistory();

    } finally {
      setLoading(false);
    }
  };

  // =====================================
  // EDITOR CHANGE
  // =====================================

  const handleEditorChange = (value) => {
    setSql(value || "");

    if (queryError) {
      setQueryError("");
    }
  };

  return (
    <div className="app">

      {/* ================= HEADER ================= */}

      <header className="header">

        <div>

          <h1>ShadowBase</h1>

          <p>
            Zero Downtime Database Migration Sandbox
          </p>

        </div>

        <div className="header-status">

          <span>Database:</span>

          <span
            className={`status-dot ${
              status.toLowerCase()
            }`}
          >
          </span>

          <strong>{status}</strong>

        </div>

      </header>

      <main className="dashboard">

        {/* ================= DATABASE ================= */}

        <section className="card">

          <h2>Shadow Database</h2>

          <div
            className={`status ${
              status.toLowerCase()
            }`}
          >
            {status}
          </div>

          <div className="buttons">

            <button
              onClick={createDatabase}
            >
              Create Database
            </button>

            <button
              onClick={checkStatus}
            >
              Check Status
            </button>

            <button
              onClick={stopDatabase}
            >
              Stop Database
            </button>

          </div>

          {message && (
            <div className="message">
              {message}
            </div>
          )}

        </section>

        {/* ================= METRICS ================= */}

        <section className="metrics-grid">

          <div className="metric-card">

            <div className="metric-title">
              Queries Replayed
            </div>

            <div className="metric-value">

              {metricsLoading
                ? "..."
                : metrics.queriesReplayed}

            </div>

            <div className="metric-description">
              Successfully replayed CDC events
            </div>

          </div>

          <div className="metric-card">

            <div className="metric-title">
              Error Rate
            </div>

            <div className="metric-value">

              {metricsLoading
                ? "..."
                : `${metrics.errorRate}%`}

            </div>

            <div className="metric-description">

              {metrics.errors} error(s) out of{" "}

              {metrics.totalEvents} event(s)

            </div>

          </div>

        </section>

        {/* ================= RECENT CDC EVENTS ================= */}

        <section className="card">

          <div className="section-header">

            <div>

              <h2>Recent CDC Events</h2>

              <p className="subtitle">
                Latest database change events
              </p>

            </div>

            <button
              onClick={getRecentEvents}
              disabled={eventsLoading}
            >
              {eventsLoading
                ? "Refreshing..."
                : "Refresh Events"}
            </button>

          </div>

          {eventsLoading &&
          recentEvents.length === 0 ? (

            <p className="empty">
              Loading CDC events...
            </p>

          ) : recentEvents.length === 0 ? (

            <p className="empty">
              No CDC events recorded yet.
            </p>

          ) : (

            <div className="events-table-container">

              <table className="events-table">

                <thead>

                  <tr>

                    <th>Operation</th>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Salary</th>
                    <th>Time</th>

                  </tr>

                </thead>

                <tbody>

                  {recentEvents.map(
                    (event, index) => (

                      <tr key={index}>

                        <td>

                          <span
                            className={`operation-badge ${String(
                              event.operation || ""
                            ).toLowerCase()}`}
                          >
                            {event.operation}
                          </span>

                        </td>

                        <td>
                          {event.id}
                        </td>

                        <td>
                          {event.name}
                        </td>

                        <td>
                          {event.salary}
                        </td>

                        <td>

                          {event.timestamp
                            ? new Date(
                                event.timestamp
                              ).toLocaleTimeString()
                            : "-"}

                        </td>

                      </tr>

                    )
                  )}

                </tbody>

              </table>

            </div>

          )}

        </section>

        {/* ================= TRAFFIC REPLAY ================= */}

        <section className="card">

          <div className="section-header">

            <div>

              <h2>Traffic Replay History</h2>

              <p className="subtitle">
                CDC operations successfully replayed
                to the Shadow Database
              </p>

            </div>

            <div className="buttons">

              <button
                onClick={getReplayHistory}
                disabled={replayLoading}
              >
                {replayLoading
                  ? "Refreshing..."
                  : "Refresh Replay"}
              </button>

              <button
                onClick={clearReplayHistory}
                disabled={
                  replayLoading ||
                  replayHistory.length === 0
                }
              >
                Clear History
              </button>

            </div>

          </div>

          {replayLoading &&
          replayHistory.length === 0 ? (

            <p className="empty">
              Loading traffic replay history...
            </p>

          ) : replayHistory.length === 0 ? (

            <p className="empty">
              No traffic replay history recorded yet.
            </p>

          ) : (

            <div className="events-table-container">

              <table className="events-table">

                <thead>

                  <tr>

                    <th>Operation</th>
                    <th>Table</th>
                    <th>ID</th>
                    <th>Time</th>

                  </tr>

                </thead>

                <tbody>

                  {replayHistory.map(
                    (replay, index) => (

                      <tr key={index}>

                        <td>

                          <span
                            className={`operation-badge ${String(
                              replay.operation || ""
                            ).toLowerCase()}`}
                          >
                            {replay.operation}
                          </span>

                        </td>

                        <td>
                          {replay.table}
                        </td>

                        <td>
                          {replay.id}
                        </td>

                        <td>

                          {replay.timestamp
                            ? new Date(
                                replay.timestamp
                              ).toLocaleTimeString()
                            : "-"}

                        </td>

                      </tr>

                    )
                  )}

                </tbody>

              </table>

            </div>

          )}

        </section>

        {/* ================= SQL EDITOR ================= */}

        <section className="card">

          <div className="section-header">

            <div>

              <h2>SQL Editor</h2>

              <p className="subtitle">

                Write a SELECT query and execute it
                against the Shadow Database.

              </p>

            </div>

            <button
              className="execute-button"
              onClick={executeSql}
              disabled={loading}
            >

              {loading
                ? "Executing..."
                : "Execute SQL"}

            </button>

          </div>

          <div className="editor-container">

            <Editor
              height="300px"
              defaultLanguage="sql"
              value={sql}
              onChange={handleEditorChange}
              theme="vs-dark"
              options={{
                minimap: {
                  enabled: false
                },
                fontSize: 15,
                automaticLayout: true,
                wordWrap: "on",
                scrollBeyondLastLine: false
              }}
            />

          </div>

          {queryError && (

            <div className="query-error">

              ❌ {queryError}

            </div>

          )}

        </section>

        {/* ================= QUERY RESULT ================= */}

        {queryResult && (

          <section className="card">

            <div className="section-header">

              <div>

                <h2>Query Result</h2>

                <p className="subtitle">

                  {queryResult.rowCount} row(s)
                  returned

                </p>

              </div>

            </div>

            {queryResult.rows &&
            queryResult.rows.length > 0 ? (

              <div className="table-container">

                <table>

                  <thead>

                    <tr>

                      {queryResult.columns.map(
                        (column) => (

                          <th key={column}>
                            {column}
                          </th>

                        )
                      )}

                    </tr>

                  </thead>

                  <tbody>

                    {queryResult.rows.map(
                      (row, rowIndex) => (

                        <tr key={rowIndex}>

                          {queryResult.columns.map(
                            (column) => (

                              <td key={column}>

                                {row[column] === null
                                  ? "NULL"
                                  : String(
                                      row[column]
                                    )}

                              </td>

                            )
                          )}

                        </tr>

                      )
                    )}

                  </tbody>

                </table>

              </div>

            ) : (

              <div className="empty-result">

                Query executed successfully,
                but no rows were returned.

              </div>

            )}

          </section>

        )}

        {/* ================= EMPLOYEES ================= */}

        <section className="card">

          <div className="section-header">

            <h2>Employees</h2>

            <button
              onClick={getEmployees}
            >
              Load Employees
            </button>

          </div>

          {employees.length === 0 ? (

            <p className="empty">
              No employees loaded.
            </p>

          ) : (

            <div className="employee-list">

              {employees.map(
                (employee, index) => (

                  <div
                    className="employee"
                    key={index}
                  >

                    {employee}

                  </div>

                )
              )}

            </div>

          )}

        </section>

      </main>

    </div>
  );
}

export default App;