import { useEffect, useState } from "react";
import "./App.css";
import LiveCharts from "./components/LiveCharts";

function App() {

  // =====================================================
  // STATE
  // =====================================================

  const [reading, setReading] = useState(null);
  const [readings, setReadings] = useState([]);
  const [dailyStats, setDailyStats] = useState(null);
  const [intelligence, setIntelligence] = useState(null);
  const [anomalyHistory, setAnomalyHistory] = useState([]);

  // NEW: Load events
  const [loadEvents, setLoadEvents] = useState([]);

  const [forecast, setForecast] = useState(null);

  const [status, setStatus] = useState("CONNECTING");
  const [error, setError] = useState(null);

  // =====================================================
  // LIVE READING FRESHNESS
  // =====================================================
  // /latest returns the last stored database row. If the
  // ESP32 stops sending data, that row can become stale.
  // Hide live values after this timeout instead of showing
  // the old reading as if it were current.
  const STALE_TIMEOUT_MS = 12000;


  // =====================================================
  // API URLS
  // =====================================================

  const BASE_URL =
    "http://localhost:8080/api/readings";

  const LATEST_API =
    `${BASE_URL}/latest`;

  const RECENT_API =
    `${BASE_URL}/recent`;

  const DAILY_STATS_API =
    `${BASE_URL}/stats/today`;

  const INTELLIGENCE_API =
    `${BASE_URL}/intelligence`;

  const ANOMALY_API =
    "http://localhost:8080/api/anomalies/recent";

  // NEW
  const LOAD_EVENTS_API =
    "http://localhost:8080/api/load-events/recent";

  const FORECAST_API =
    `${BASE_URL}/forecast`;


  // =====================================================
  // FETCH LATEST READING
  // =====================================================

  const fetchLatestReading = async () => {

    try {

      const response =
        await fetch(LATEST_API, {
          cache: "no-store"
        });

      if (!response.ok) {

        throw new Error(
          `Latest reading HTTP error: ${response.status}`
        );

      }

      const data =
        await response.json();

      // /latest gives the last DATABASE reading.
      // Check its timestamp to determine whether the ESP32
      // is actually sending fresh data.
      const readingTime =
        data?.createdAt
          ? new Date(data.createdAt).getTime()
          : NaN;

      const age =
        Number.isFinite(readingTime)
          ? Date.now() - readingTime
          : Infinity;

      if (age > STALE_TIMEOUT_MS) {

        // Keep identity and timestamp for context, but clear
        // all live sensor values so stale data is never shown
        // as current.
        setReading({
          ...data,
          voltage: null,
          current: null,
          power: null,
          energy: null,
          frequency: null,
          powerFactor: null,
          tariffCost: null,
          wifiStrength: null
        });

        setStatus("OFFLINE");

        setError(
          `No new ESP32 reading received for ${Math.round(
            age / 1000
          )} seconds`
        );

        return;
      }

      // Fresh reading received.
      setReading(data);
      setStatus("LIVE");
      setError(null);

    } catch (err) {

      console.error(
        "Failed to fetch latest reading:",
        err
      );

      setStatus("OFFLINE");

      setError(
        "Unable to connect to SmartEdge backend"
      );
    }
  };

  // =====================================================
  // FETCH RECENT READINGS
  // =====================================================

  const fetchRecentReadings = async () => {

    try {

      const response =
        await fetch(RECENT_API);

      if (!response.ok) {

        throw new Error(
          `Recent readings HTTP error: ${response.status}`
        );
      }

      const data =
        await response.json();

      if (Array.isArray(data)) {

        setReadings(data);
      }

    } catch (err) {

      console.error(
        "Failed to fetch recent readings:",
        err
      );
    }
  };


  // =====================================================
  // FETCH DAILY STATISTICS
  // =====================================================

  const fetchDailyStats = async () => {

    try {

      const response =
        await fetch(DAILY_STATS_API);

      if (!response.ok) {

        throw new Error(
          `Daily stats HTTP error: ${response.status}`
        );
      }

      const data =
        await response.json();

      setDailyStats(data);

    } catch (err) {

      console.error(
        "Failed to fetch daily statistics:",
        err
      );
    }
  };


  // =====================================================
  // FETCH INTELLIGENCE
  // =====================================================

  const fetchIntelligence = async () => {

    try {

      const response =
        await fetch(INTELLIGENCE_API);

      if (!response.ok) {

        throw new Error(
          `Intelligence HTTP error: ${response.status}`
        );
      }

      const data =
        await response.json();

      setIntelligence(data);

    } catch (err) {

      console.error(
        "Failed to fetch SmartEdge intelligence:",
        err
      );
    }
  };


  // =====================================================
  // FETCH ANOMALY HISTORY
  // =====================================================

  const fetchAnomalyHistory = async () => {

    try {

      const response =
        await fetch(ANOMALY_API);

      if (!response.ok) {

        throw new Error(
          `Anomaly history HTTP error: ${response.status}`
        );
      }

      const data =
        await response.json();

      if (Array.isArray(data)) {

        setAnomalyHistory(data);
      }

    } catch (err) {

      console.error(
        "Failed to fetch anomaly history:",
        err
      );
    }
  };


  // =====================================================
  // FETCH LOAD EVENTS
  // =====================================================

  const fetchLoadEvents = async () => {

    try {

      const response =
        await fetch(LOAD_EVENTS_API);

      if (!response.ok) {

        throw new Error(
          `Load events HTTP error: ${response.status}`
        );
      }

      const data =
        await response.json();

      if (Array.isArray(data)) {

        setLoadEvents(data);
      }

    } catch (err) {

      console.error(
        "Failed to fetch load events:",
        err
      );
    }
  };


  // =====================================================
  // FETCH FORECAST
  // =====================================================

  const fetchForecast = async () => {

    try {

      const response =
        await fetch(FORECAST_API);

      if (!response.ok) {

        throw new Error(
          `Forecast HTTP error: ${response.status}`
        );
      }

      const data =
        await response.json();

      setForecast(data);

    } catch (err) {

      console.error(
        "Failed to fetch forecast:",
        err
      );
    }
  };


  // =====================================================
  // AUTO REFRESH
  // =====================================================

  useEffect(() => {

    const fetchDashboardData = () => {

      fetchLatestReading();

      fetchRecentReadings();

      fetchDailyStats();

      fetchIntelligence();

      fetchAnomalyHistory();

      fetchLoadEvents();

      fetchForecast();
    };


    // Fetch immediately
    fetchDashboardData();


    // Refresh every 5 seconds
    const interval =
      setInterval(
        fetchDashboardData,
        5000
      );


    return () =>
      clearInterval(interval);

  }, []);


  // =====================================================
  // STATUS CLASS
  // =====================================================

  const getStatusClass = (value) => {

    if (!value) {

      return "status-unknown";
    }

    const normalized =
      value.toUpperCase();


    if (
      normalized === "NORMAL" ||
      normalized === "GOOD" ||
      normalized === "EXCELLENT"
    ) {

      return "status-good";
    }


    if (
      normalized === "WARNING" ||
      normalized === "FAIR"
    ) {

      return "status-warning";
    }


    if (
      normalized === "HIGH" ||
      normalized === "LOW" ||
      normalized === "POOR" ||
      normalized === "ABNORMAL" ||
      normalized === "CRITICAL" ||
      normalized === "NO SUPPLY"
    ) {

      return "status-danger";
    }


    return "status-unknown";
  };


  // =====================================================
  // HEALTH CLASS
  // =====================================================

  const getHealthClass = (health) => {

    if (!health) {

      return "health-unknown";
    }


    switch (
      health.toUpperCase()
    ) {

      case "NORMAL":

        return "health-normal";


      case "WARNING":

        return "health-warning";


      case "CRITICAL":

        return "health-critical";


      case "AC OFFLINE":

        return "health-critical";


      default:

        return "health-unknown";
    }
  };


  // =====================================================
  // FORMAT ANOMALY TYPE
  // =====================================================

  const formatAnomalyType = (type) => {

    if (!type) {

      return "UNKNOWN";
    }

    return type.replaceAll(
      "_",
      " "
    );
  };


  // =====================================================
  // FORMAT DATE
  // =====================================================

  const formatDate = (date) => {

    if (!date) {

      return "--";
    }

    return new Date(
      date
    ).toLocaleString();
  };


  // =====================================================
  // FORMAT LOAD CHANGE
  // =====================================================

  const formatLoadChange = (value) => {

    if (value == null) {

      return "--";
    }

    const number =
      Number(value);

    if (number > 0) {

      return `+${number.toFixed(2)} W`;
    }

    return `${number.toFixed(2)} W`;
  };


  // =====================================================
  // FORMAT APPLIANCE TYPE
  // =====================================================

  const formatApplianceType = (type) => {

    if (!type) {
      return "Classification unavailable";
    }

    switch (type) {

      case "SMALL_ELECTRONIC_LOAD":
        return "Possible LED / Small Electronic Load";

      case "POSSIBLE_FAN":
        return "Possible Fan";

      case "MEDIUM_APPLIANCE":
        return "Possible Medium Appliance";

      case "LARGE_APPLIANCE":
        return "Possible Large Appliance";

      case "HIGH_POWER_APPLIANCE":
        return "Possible High-Power Appliance";

      case "HEAVY_LOAD":
        return "Possible Heavy Load";

      case "UNKNOWN_SMALL_LOAD":
        return "Unknown Small Load";

      default:
        return type.replaceAll("_", " ");
    }
  };


  // =====================================================
  // FORMAT CLASSIFICATION METHOD
  // =====================================================

  const formatClassificationMethod = (method) => {

    if (!method) {
      return "--";
    }

    return method.replaceAll("_", " ");
  };


  // =====================================================
  // CONFIDENCE PERCENTAGE
  // =====================================================

  const getConfidencePercentage = (confidence) => {

    if (confidence == null) {
      return 0;
    }

    const value =
      Number(confidence) * 100;

    return Math.max(
      0,
      Math.min(100, value)
    );
  };


  // =====================================================
  // LOADING SCREEN
  // =====================================================

  if (!reading) {

    return (

      <div className="dashboard">

        <div className="header">

          <div>

            <h1>
              ⚡ SmartEdge
            </h1>

            <p>
              Intelligent Energy Monitoring System
            </p>

          </div>


          <span
            className={`status ${status.toLowerCase()}`}
          >
            {status}
          </span>

        </div>


        <div className="loading">

          {error
            ? error
            : "Waiting for energy data..."}

        </div>

      </div>
    );
  }


  // =====================================================
  // MAIN DASHBOARD
  // =====================================================

  return (

    <div className="dashboard">


      {/* =================================================
          HEADER
      ================================================= */}

      <div className="header">

        <div>

          <h1>
            ⚡ SmartEdge
          </h1>

          <p>
            Intelligent Energy Monitoring System
          </p>

        </div>


        <span
          className={`status ${status.toLowerCase()}`}
        >
          ● {status}
        </span>

      </div>


      {/* =================================================
          DEVICE INFORMATION
      ================================================= */}

      <div className="device-info">

        <span>

          Device:{" "}

          <strong>
            {reading.deviceId ||
              "ESP32-001"}
          </strong>

        </span>


        <span>

          WiFi:{" "}

          <strong>
            {reading.wifiStrength ??
              "--"} dBm
          </strong>

        </span>


        <span>

          Firmware:{" "}

          <strong>
            {reading.firmwareVersion ||
              "--"}
          </strong>

        </span>

      </div>

      {status === "OFFLINE" && (
        <div className="stale-banner">
          <span className="stale-banner-icon">⚠️</span>
          <div>
            <strong>ESP32 is offline</strong>
            <p>
              No fresh sensor reading has been received.
              Live values are temporarily hidden to avoid showing stale data.
            </p>
          </div>
        </div>
      )}


      {/* =================================================
          LIVE SENSOR CARDS
      ================================================= */}

      <div className="cards">


        {/* VOLTAGE */}

        <div className="card">

          <div className="card-icon">
            ⚡
          </div>

          <h3>
            Voltage
          </h3>

          <div className="value">

            {reading.voltage != null
              ? Number(
                  reading.voltage
                ).toFixed(1)
              : "--"}

            <span>V</span>

          </div>

        </div>


        {/* CURRENT */}

        <div className="card">

          <div className="card-icon">
            🔌
          </div>

          <h3>
            Current
          </h3>

          <div className="value">

            {reading.current != null
              ? Number(
                  reading.current
                ).toFixed(3)
              : "--"}

            <span>A</span>

          </div>

        </div>


        {/* POWER */}

        <div className="card">

          <div className="card-icon">
            💡
          </div>

          <h3>
            Power
          </h3>

          <div className="value">

            {reading.power != null
              ? Number(
                  reading.power
                ).toFixed(1)
              : "--"}

            <span>W</span>

          </div>

        </div>


        {/* ENERGY */}

        <div className="card">

          <div className="card-icon">
            🔋
          </div>

          <h3>
            Energy
          </h3>

          <div className="value">

            {reading.energy != null
              ? Number(
                  reading.energy
                ).toFixed(3)
              : "--"}

            <span>kWh</span>

          </div>

        </div>


        {/* FREQUENCY */}

        <div className="card">

          <div className="card-icon">
            〰️
          </div>

          <h3>
            Frequency
          </h3>

          <div className="value">

            {reading.frequency != null
              ? Number(
                  reading.frequency
                ).toFixed(1)
              : "--"}

            <span>Hz</span>

          </div>

        </div>


        {/* POWER FACTOR */}

        <div className="card">

          <div className="card-icon">
            📊
          </div>

          <h3>
            Power Factor
          </h3>

          <div className="value">

            {reading.powerFactor != null
              ? Number(
                  reading.powerFactor
                ).toFixed(2)
              : "--"}

          </div>

        </div>

      </div>


      {/* =================================================
          COST + CONNECTION
      ================================================= */}

      <div className="bottom-cards">


        <div className="summary-card">

          <h3>
            💰 Current Meter Cost
          </h3>

          <div className="cost">

            ₹
            {reading.tariffCost != null
              ? Number(
                  reading.tariffCost
                ).toFixed(2)
              : "0.00"}

          </div>

          <p>
            Based on cumulative PZEM energy
          </p>

        </div>


        <div className="summary-card">

          <h3>
            📡 ESP32 Connection
          </h3>

          <div
            className={
              status === "LIVE"
                ? "connection"
                : "connection disconnected"
            }
          >

            {status === "LIVE"
              ? "Connected"
              : "Disconnected"}

          </div>

          <p>

            Signal Strength:{" "}

            {reading.wifiStrength ??
              "--"} dBm

          </p>

        </div>

      </div>


      {/* =================================================
          TODAY'S ENERGY SUMMARY
      ================================================= */}

      <section className="daily-section">

        <div className="section-heading">

          <div>

            <h2>
              📊 Today's Energy Summary
            </h2>

            <p>
              Calculated from today's valid
              SmartEdge sensor readings
            </p>

          </div>


          {dailyStats && (

            <div className="reading-count">

              {dailyStats.totalReadings ??
                0}{" "}

              valid readings

            </div>

          )}

        </div>


        {dailyStats ? (

          <div className="daily-grid">


            <div className="daily-card">

              <div className="daily-icon">
                ⚡
              </div>

              <div className="daily-label">
                Energy Used
              </div>

              <div className="daily-value energy-value">

                {dailyStats.energyUsed != null
                  ? Number(
                      dailyStats.energyUsed
                    ).toFixed(4)
                  : "0.0000"}

                <span>kWh</span>

              </div>

            </div>


            <div className="daily-card">

              <div className="daily-icon">
                💡
              </div>

              <div className="daily-label">
                Average Power
              </div>

              <div className="daily-value power-value">

                {dailyStats.averagePower != null
                  ? Number(
                      dailyStats.averagePower
                    ).toFixed(2)
                  : "0.00"}

                <span>W</span>

              </div>

            </div>


            <div className="daily-card">

              <div className="daily-icon">
                🔥
              </div>

              <div className="daily-label">
                Peak Power
              </div>

              <div className="daily-value peak-value">

                {dailyStats.peakPower != null
                  ? Number(
                      dailyStats.peakPower
                    ).toFixed(2)
                  : "0.00"}

                <span>W</span>

              </div>

            </div>


            <div className="daily-card">

              <div className="daily-icon">
                ⚡
              </div>

              <div className="daily-label">
                Average Voltage
              </div>

              <div className="daily-value voltage-value">

                {dailyStats.averageVoltage != null
                  ? Number(
                      dailyStats.averageVoltage
                    ).toFixed(2)
                  : "0.00"}

                <span>V</span>

              </div>

            </div>


            <div className="daily-card">

              <div className="daily-icon">
                🔌
              </div>

              <div className="daily-label">
                Average Current
              </div>

              <div className="daily-value current-value">

                {dailyStats.averageCurrent != null
                  ? Number(
                      dailyStats.averageCurrent
                    ).toFixed(3)
                  : "0.000"}

                <span>A</span>

              </div>

            </div>


            <div className="daily-card">

              <div className="daily-icon">
                💰
              </div>

              <div className="daily-label">
                Estimated Cost
              </div>

              <div className="daily-value cost-value">

                ₹
                {dailyStats.estimatedCost != null
                  ? Number(
                      dailyStats.estimatedCost
                    ).toFixed(2)
                  : "0.00"}

              </div>

            </div>

          </div>

        ) : (

          <div className="daily-loading">

            Loading today's analytics...

          </div>

        )}

      </section>


      {/* =================================================
          SMARTEDGE INTELLIGENCE
      ================================================= */}

      <section className="intelligence-section">

        <div className="intelligence-header">

          <div>

            <h2>
              🧠 SmartEdge Intelligence
            </h2>

            <p>
              Real-time electrical health and
              anomaly detection
            </p>

          </div>


          {intelligence && (

            <div
              className={`health-badge ${getHealthClass(
                intelligence.systemHealth
              )}`}
            >

              ● {intelligence.systemHealth}

            </div>

          )}

        </div>


        {intelligence ? (

          <>


            {/* OVERALL HEALTH */}

            <div className="system-health-card">

              <div className="health-icon">

                {intelligence.systemHealth ===
                "NORMAL"

                  ? "🛡️"

                  : intelligence.systemHealth ===
                    "AC OFFLINE"

                  ? "🔌"

                  : intelligence.systemHealth ===
                    "WARNING"

                  ? "⚠️"

                  : "🚨"}

              </div>


              <div className="health-content">

                <span>
                  Overall System Health
                </span>


                <h3
                  className={getHealthClass(
                    intelligence.systemHealth
                  )}
                >

                  {intelligence.systemHealth}

                </h3>


                <p>

                  {intelligence.systemHealth ===
                  "AC OFFLINE"

                    ? "AC supply is currently unavailable."

                    : intelligence.anomalyCount ===
                      0

                    ? "All monitored electrical parameters are operating normally."

                    : `${intelligence.anomalyCount} anomaly${
                        intelligence.anomalyCount ===
                        1
                          ? ""
                          : "ies"
                      } detected.`}

                </p>

              </div>

            </div>


            {/* PARAMETER STATUS */}

            <div className="intelligence-status-grid">


              <div className="intelligence-status-card">

                <div className="parameter-icon">
                  ⚡
                </div>

                <div>

                  <span>
                    Voltage
                  </span>

                  <strong
                    className={getStatusClass(
                      intelligence.voltageStatus
                    )}
                  >
                    {intelligence.voltageStatus}
                  </strong>

                </div>

              </div>


              <div className="intelligence-status-card">

                <div className="parameter-icon">
                  🔌
                </div>

                <div>

                  <span>
                    Current
                  </span>

                  <strong
                    className={getStatusClass(
                      intelligence.currentStatus
                    )}
                  >
                    {intelligence.currentStatus}
                  </strong>

                </div>

              </div>


              <div className="intelligence-status-card">

                <div className="parameter-icon">
                  💡
                </div>

                <div>

                  <span>
                    Power
                  </span>

                  <strong
                    className={getStatusClass(
                      intelligence.powerStatus
                    )}
                  >
                    {intelligence.powerStatus}
                  </strong>

                </div>

              </div>


              <div className="intelligence-status-card">

                <div className="parameter-icon">
                  〰️
                </div>

                <div>

                  <span>
                    Frequency
                  </span>

                  <strong
                    className={getStatusClass(
                      intelligence.frequencyStatus
                    )}
                  >
                    {intelligence.frequencyStatus}
                  </strong>

                </div>

              </div>


              <div className="intelligence-status-card">

                <div className="parameter-icon">
                  📊
                </div>

                <div>

                  <span>
                    Power Factor
                  </span>

                  <strong
                    className={getStatusClass(
                      intelligence.powerFactorStatus
                    )}
                  >
                    {intelligence.powerFactorStatus}
                  </strong>

                </div>

              </div>


              <div className="intelligence-status-card">

                <div className="parameter-icon">
                  📶
                </div>

                <div>

                  <span>
                    WiFi
                  </span>

                  <strong
                    className={getStatusClass(
                      intelligence.wifiStatus
                    )}
                  >
                    {intelligence.wifiStatus}
                  </strong>

                </div>

              </div>

            </div>


            {/* ANOMALIES + RECOMMENDATIONS */}

            <div className="intelligence-details-grid">


              <div className="intelligence-detail-card">

                <div className="detail-title">

                  <div>

                    <span className="detail-icon">
                      🛡️
                    </span>

                    <h3>
                      Anomaly Detection
                    </h3>

                  </div>


                  <span
                    className={`anomaly-counter ${
                      intelligence.anomalyCount >
                      0
                        ? "has-anomaly"
                        : ""
                    }`}
                  >

                    {intelligence.anomalyCount}

                  </span>

                </div>


                {intelligence.anomalies &&
                intelligence.anomalies.length >
                  0 ? (

                  <div className="anomaly-list">

                    {intelligence.anomalies.map(
                      (anomaly, index) => (

                        <div
                          className="anomaly-item"
                          key={index}
                        >

                          <span>
                            ⚠️
                          </span>

                          <p>
                            {anomaly}
                          </p>

                        </div>

                      )
                    )}

                  </div>

                ) : (

                  <div className="no-anomaly">

                    <span className="check-circle">
                      ✓
                    </span>

                    <div>

                      <strong>
                        No anomalies detected
                      </strong>

                      <p>
                        All monitored parameters are
                        within the configured ranges.
                      </p>

                    </div>

                  </div>

                )}

              </div>


              {/* RECOMMENDATIONS */}

              <div className="intelligence-detail-card">

                <div className="detail-title">

                  <div>

                    <span className="detail-icon">
                      💡
                    </span>

                    <h3>
                      Smart Recommendations
                    </h3>

                  </div>

                </div>


                <div className="recommendation-list">

                  {intelligence.recommendations &&
                  intelligence.recommendations
                    .length > 0 ? (

                    intelligence.recommendations.map(
                      (
                        recommendation,
                        index
                      ) => (

                        <div
                          className="recommendation-item"
                          key={index}
                        >

                          <span>
                            ✓
                          </span>

                          <p>
                            {recommendation}
                          </p>

                        </div>

                      )
                    )

                  ) : (

                    <p className="empty-message">
                      No recommendations available.
                    </p>

                  )}

                </div>

              </div>

            </div>

          </>

        ) : (

          <div className="intelligence-loading">

            <div className="brain-loader">
              🧠
            </div>

            <p>
              Analysing SmartEdge sensor data...
            </p>

          </div>

        )}

      </section>


      {/* =================================================
          ANOMALY HISTORY
      ================================================= */}

      <section className="anomaly-history-section">

        <div className="anomaly-history-header">

          <div>

            <h2>
              ⚠️ Recent Anomaly History
            </h2>

            <p>
              Previously detected SmartEdge
              electrical and connectivity anomalies
            </p>

          </div>


          <div className="history-count">

            {anomalyHistory.length}{" "}

            {anomalyHistory.length === 1
              ? "Event"
              : "Events"}

          </div>

        </div>


        {anomalyHistory.length > 0 ? (

          <div className="anomaly-table-wrapper">

            <table className="anomaly-table">

              <thead>

                <tr>

                  <th>
                    Time
                  </th>

                  <th>
                    Type
                  </th>

                  <th>
                    Parameter
                  </th>

                  <th>
                    Value
                  </th>

                  <th>
                    Severity
                  </th>

                  <th>
                    Message
                  </th>

                </tr>

              </thead>


              <tbody>

                {anomalyHistory.map(
                  (event) => (

                    <tr key={event.id}>

                      <td className="anomaly-time">

                        {formatDate(
                          event.detectedAt
                        )}

                      </td>


                      <td>

                        <span className="anomaly-type">

                          {formatAnomalyType(
                            event.anomalyType
                          )}

                        </span>

                      </td>


                      <td>

                        {event.parameterName ||
                          "--"}

                      </td>


                      <td className="anomaly-value">

                        {event.measuredValue != null
                          ? Number(
                              event.measuredValue
                            ).toFixed(2)
                          : "--"}

                        {event.unit
                          ? ` ${event.unit}`
                          : ""}

                      </td>


                      <td>

                        <span
                          className={`severity-badge ${
                            event.severity ===
                            "CRITICAL"

                              ? "severity-critical"

                              : event.severity ===
                                "WARNING"

                              ? "severity-warning"

                              : "severity-info"
                          }`}
                        >

                          {event.severity ||
                            "UNKNOWN"}

                        </span>

                      </td>


                      <td className="anomaly-message">

                        {event.message ||
                          "--"}

                      </td>

                    </tr>

                  )
                )}

              </tbody>

            </table>

          </div>

        ) : (

          <div className="empty-anomaly-history">

            <div className="empty-history-icon">
              🛡️
            </div>

            <div>

              <h3>
                No anomaly history
              </h3>

              <p>
                No electrical or connectivity
                anomalies have been recorded yet.
              </p>

            </div>

          </div>

        )}

      </section>


      {/* =================================================
          SMART ENERGY FORECAST
      ================================================= */}

      <section className="forecast-section">
        <div className="forecast-header">
          <div>
            <h2>🔮 Smart Energy Forecast</h2>
            <p>Short-term power consumption prediction from recent SmartEdge readings</p>
          </div>

          <div className={`forecast-trend-badge ${
            forecast?.trend === "INCREASING"
              ? "forecast-increasing"
              : forecast?.trend === "DECREASING"
              ? "forecast-decreasing"
              : forecast?.trend === "STABLE"
              ? "forecast-stable"
              : "forecast-waiting"
          }`}>
            {forecast?.trend || "WAITING"}
          </div>
        </div>

        {forecast ? (
          <>
            <div className="forecast-metrics">
              <div className="forecast-metric">
                <span>Current Average</span>
                <strong>{Number(forecast.currentAveragePower ?? 0).toFixed(2)} W</strong>
              </div>

              <div className="forecast-metric forecast-primary">
                <span>Predicted Power</span>
                <strong>{Number(forecast.predictedPower ?? 0).toFixed(2)} W</strong>
              </div>

              <div className="forecast-metric">
                <span>Predicted Change</span>
                <strong>
                  {Number(forecast.powerDifference ?? 0) > 0 ? "+" : ""}
                  {Number(forecast.powerDifference ?? 0).toFixed(2)} W
                </strong>
              </div>
            </div>

            <div className="forecast-confidence">
              <div className="forecast-confidence-row">
                <span>Prediction Confidence</span>
                <strong>
                  {Math.round(Math.max(0, Math.min(1, Number(forecast.confidence ?? 0))) * 100)}%
                </strong>
              </div>

              <div className="forecast-confidence-track">
                <div
                  className="forecast-confidence-fill"
                  style={{
                    width: `${Math.max(0, Math.min(1, Number(forecast.confidence ?? 0))) * 100}%`
                  }}
                />
              </div>
            </div>

            <div className="forecast-insight">
              <div className="forecast-insight-icon">🧠</div>
              <div className="forecast-insight-content">
                <div className="forecast-method">
                  {forecast.predictionMethod
                    ? forecast.predictionMethod.replaceAll("_", " ")
                    : "WAITING FOR DATA"}
                </div>
                <p>
                  {forecast.recommendation ||
                    "SmartEdge is collecting data for forecasting."}
                </p>
              </div>
            </div>
          </>
        ) : (
          <div className="forecast-empty">
            SmartEdge is preparing the energy forecast...
          </div>
        )}
      </section>


      {/* =================================================
          LIVE LOAD ACTIVITY + SMART CLASSIFICATION
      ================================================= */}

      <section className="load-activity-section">

        <div className="load-activity-header">

          <div>

            <h2>
              ⚡ Live Load Activity
            </h2>

            <p>
              Power changes and intelligent load
              classification from SmartEdge
            </p>

          </div>


          <div className="load-event-count">

            {loadEvents.length}{" "}

            {loadEvents.length === 1
              ? "Event"
              : "Events"}

          </div>

        </div>


        {loadEvents.length > 0 ? (

          <div className="load-events-list">

            {loadEvents.map((event) => {

              const isLoadOn =
                event.eventType === "LOAD_ON";

              const hasClassification =
                event.applianceType != null;

              const confidencePercentage =
                getConfidencePercentage(
                  event.confidence
                );

              return (

                <div
                  className={`load-event-card ${
                    isLoadOn
                      ? "load-on-card"
                      : "load-off-card"
                  }`}
                  key={event.id}
                >

                  <div
                    className={`load-event-icon ${
                      isLoadOn
                        ? "load-on-icon"
                        : "load-off-icon"
                    }`}
                  >

                    {isLoadOn
                      ? "⚡"
                      : "🔌"}

                  </div>


                  <div className="load-event-content">

                    <div className="load-event-top">

                      <div>

                        <span
                          className={`load-event-type ${
                            isLoadOn
                              ? "load-on-text"
                              : "load-off-text"
                          }`}
                        >

                          {isLoadOn
                            ? "LOAD ON"
                            : "LOAD OFF"}

                        </span>


                        <h3>

                          {isLoadOn
                            ? "Power consumption increased"
                            : "Power consumption decreased"}

                        </h3>

                      </div>


                      <div
                        className={`load-change ${
                          isLoadOn
                            ? "positive-load-change"
                            : "negative-load-change"
                        }`}
                      >

                        {formatLoadChange(
                          event.powerChange
                        )}

                      </div>

                    </div>


                    <div className="load-power-transition">

                      <div>

                        <span>
                          Previous
                        </span>

                        <strong>

                          {event.previousPower != null
                            ? Number(
                                event.previousPower
                              ).toFixed(1)
                            : "--"}{" "}

                          W

                        </strong>

                      </div>


                      <div className="load-arrow">
                        →
                      </div>


                      <div>

                        <span>
                          Current
                        </span>

                        <strong>

                          {event.currentPower != null
                            ? Number(
                                event.currentPower
                              ).toFixed(1)
                            : "--"}{" "}

                          W

                        </strong>

                      </div>

                    </div>


                    {hasClassification ? (

                      <div className="load-classification">

                        <div className="classification-header">

                          <div className="classification-icon">
                            🧠
                          </div>

                          <div>

                            <span className="classification-label">
                              Smart Load Classification
                            </span>

                            <h4>
                              {formatApplianceType(
                                event.applianceType
                              )}
                            </h4>

                          </div>

                        </div>


                        <div className="classification-details">

                          <div className="classification-row">

                            <span>
                              Confidence
                            </span>

                            <strong>
                              {confidencePercentage.toFixed(0)}%
                            </strong>

                          </div>


                          <div className="confidence-track">

                            <div
                              className="confidence-fill"
                              style={{
                                width:
                                  `${confidencePercentage}%`
                              }}
                            />

                          </div>


                          <div className="classification-row method-row">

                            <span>
                              Classification Method
                            </span>

                            <strong className="classification-method">

                              {formatClassificationMethod(
                                event.classificationMethod
                              )}

                            </strong>

                          </div>

                        </div>

                      </div>

                    ) : (

                      <div className="classification-unavailable">

                        <span>
                          🧠
                        </span>

                        <div>

                          <strong>
                            Classification unavailable
                          </strong>

                          <p>
                            This event was recorded before
                            Smart Load Classification was enabled.
                          </p>

                        </div>

                      </div>

                    )}


                    <div className="load-event-meta">

                      <span>

                        📟{" "}

                        {event.deviceId ||
                          "Unknown Device"}

                      </span>


                      <span>

                        🕒{" "}

                        {formatDate(
                          event.detectedAt
                        )}

                      </span>


                      <span>
                        #{event.id}
                      </span>

                    </div>

                  </div>

                </div>

              );
            })}

          </div>

        ) : (

          <div className="empty-load-events">

            <div className="empty-load-icon">
              ⚡
            </div>


            <div>

              <h3>
                No load changes detected
              </h3>

              <p>
                SmartEdge is monitoring for
                significant changes in power
                consumption.
              </p>

            </div>

          </div>

        )}

      </section>


      {/* =================================================
          LIVE CHARTS
      ================================================= */}

      <LiveCharts
        readings={readings}
      />


      {/* =================================================
          LAST UPDATE
      ================================================= */}

      <div className="last-update">

        <span>

          {status === "LIVE"
            ? `Latest Reading #${reading.id ?? "--"}`
            : "Waiting for fresh ESP32 reading"}

        </span>


        <span>

          {reading.createdAt

            ? `Last sensor update: ${new Date(
                reading.createdAt
              ).toLocaleString()}`

            : "Time unavailable"}

        </span>

      </div>

    </div>
  );
}

export default App;
