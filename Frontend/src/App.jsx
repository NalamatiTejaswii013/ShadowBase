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
  // AUTOMATIC STATUS CHECK
  // =====================================
  useEffect(() => {
    checkStatus();
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

      // Clear old SQL result
      setQueryResult(null);

      // Clear employees
      setEmployees([]);

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

    } finally {

      setLoading(false);
    }
  };

  // =====================================
  // EDITOR CHANGE
  // =====================================
  const handleEditorChange = (value) => {

    setSql(value || "");

    // Clear previous error when user edits SQL
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


          {/* SQL ERROR */}

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