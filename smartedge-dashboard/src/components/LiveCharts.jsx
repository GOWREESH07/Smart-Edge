import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

function LiveCharts({ readings }) {
  if (!readings || readings.length === 0) {
    return (
      <div className="charts-section">
        <h2>Live Energy Analytics</h2>
        <p>Waiting for energy readings...</p>
      </div>
    );
  }

  // Remove obviously corrupted sensor readings
  const validReadings = readings.filter((reading) => {
    const voltage = Number(reading.voltage);
    const current = Number(reading.current);
    const power = Number(reading.power);
    const energy = Number(reading.energy);

    return (
      Number.isFinite(voltage) &&
      Number.isFinite(current) &&
      Number.isFinite(power) &&
      Number.isFinite(energy) &&
      voltage >= 180 &&
      voltage <= 270 &&
      current >= 0 &&
      current <= 20 &&
      power >= 0 &&
      power <= 5000 &&
      energy >= 0
    );
  });

  const chartData = validReadings.map((reading) => ({
    id: reading.id,

    time: new Date(reading.createdAt).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    }),

    voltage: Number(reading.voltage),
    current: Number(reading.current),
    power: Number(reading.power),
    energy: Number(reading.energy),
  }));

  if (chartData.length === 0) {
    return (
      <div className="charts-section">
        <h2>Live Energy Analytics</h2>
        <p>Waiting for valid PZEM readings...</p>
      </div>
    );
  }

  return (
    <div className="charts-section">

      <h2>Live Energy Analytics</h2>

      <div className="charts-grid">

        {/* POWER */}

        <ChartCard
          title="💡 Power Consumption"
          data={chartData}
          dataKey="power"
          name="Power"
          unit="W"
          stroke="#ff8c42"
        />

        {/* VOLTAGE */}

        <ChartCard
          title="⚡ Voltage History"
          data={chartData}
          dataKey="voltage"
          name="Voltage"
          unit="V"
          stroke="#facc15"
        />

        {/* CURRENT */}

        <ChartCard
          title="🔌 Current Consumption"
          data={chartData}
          dataKey="current"
          name="Current"
          unit="A"
          stroke="#38bdf8"
        />

        {/* ENERGY */}

        <ChartCard
          title="🔋 Energy Consumption"
          data={chartData}
          dataKey="energy"
          name="Energy"
          unit="kWh"
          stroke="#4ade80"
        />

      </div>

    </div>
  );
}


/* ============================================
   REUSABLE CHART COMPONENT
============================================ */

function ChartCard({
  title,
  data,
  dataKey,
  name,
  unit,
  stroke,
}) {
  return (
    <div className="chart-card">

      <h3>{title}</h3>

      <ResponsiveContainer width="100%" height={300}>

        <LineChart
          data={data}
          margin={{
            top: 10,
            right: 20,
            left: 10,
            bottom: 10,
          }}
        >

          <CartesianGrid
            strokeDasharray="3 3"
            stroke="#334155"
          />

          <XAxis
            dataKey="time"
            stroke="#94a3b8"
            tick={{ fontSize: 12 }}
          />

          <YAxis
            stroke="#94a3b8"
            domain={["auto", "auto"]}
            tick={{ fontSize: 12 }}
            width={60}
          />

          <Tooltip
            formatter={(value) => [
              `${Number(value).toFixed(3)} ${unit}`,
              name,
            ]}
            contentStyle={{
              backgroundColor: "#0f172a",
              border: "1px solid #334155",
              borderRadius: "8px",
            }}
          />

          <Line
            type="monotone"
            dataKey={dataKey}
            name={name}
            stroke={stroke}
            strokeWidth={3}
            dot={false}
            activeDot={{ r: 5 }}
            isAnimationActive={false}
          />

        </LineChart>

      </ResponsiveContainer>

      <span className="chart-unit">
        {name} ({unit})
      </span>

    </div>
  );
}

export default LiveCharts;