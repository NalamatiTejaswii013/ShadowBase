import { useState } from "react";
import axios from "axios";
import "./App.css";

const API_URL = "http://localhost:8080/api/shadow";

function App() {
  const [status, setStatus] = useState("UNKNOWN");
  const [message, setMessage] = useState("");
  const [employees, setEmployees] = useState([]);

  const createDatabase = async () => {
    try {
      const response = await axios.post(`${API_URL}/create`);
      setMessage(response.data);
      setStatus("RUNNING");
    } catch (error) {
      setMessage("Failed to create Shadow Database.");
      console.error(error);
    }
  };

  const checkStatus = async () => {
    try {
      const response = await axios.get(`${API_URL}/status`);
      setMessage(response.data);

      if (response.data.includes("RUNNING")) {
        setStatus("RUNNING");
      } else {
        setStatus("STOPPED");
      }
    } catch (error) {
      setMessage("Failed to check database status.");
      console.error(error);
    }
  };

  const stopDatabase = async () => {
    try {
      const response = await axios.post(`${API_URL}/stop`);
      setMessage(response.data);
      setStatus("STOPPED");
    } catch (error) {
      setMessage("Failed to stop Shadow Database.");
      console.error(error);
    }
  };

  const getEmployees = async () => {
    try {
      const response = await axios.get(`${API_URL}/employees`);
      setEmployees(response.data);
      setMessage("Employees loaded successfully.");
    } catch (error) {
      setMessage("Failed to load employees.");
      console.error(error);
    }
  };

  return (
    <div className="app">

      <header className="header">
        <h1>ShadowBase</h1>
        <p>Zero Downtime Database Migration Sandbox</p>
      </header>

      <main className="dashboard">

        <section className="status-card">
          <h2>Shadow Database</h2>

          <div className={`status ${status.toLowerCase()}`}>
            {status}
          </div>

          <div className="buttons">
            <button onClick={createDatabase}>
              Create Database
            </button>

            <button onClick={checkStatus}>
              Check Status
            </button>

            <button onClick={stopDatabase}>
              Stop Database
            </button>
          </div>

          {message && (
            <div className="message">
              {message}
            </div>
          )}
        </section>

        <section className="employees-card">
          <div className="section-header">
            <h2>Employees</h2>

            <button onClick={getEmployees}>
              Load Employees
            </button>
          </div>

          {employees.length === 0 ? (
            <p className="empty">
              No employees loaded.
            </p>
          ) : (
            <div className="employee-list">

              {employees.map((employee, index) => (
                <div className="employee" key={index}>
                  {employee}
                </div>
              ))}

            </div>
          )}
        </section>

      </main>

    </div>
  );
}

export default App;