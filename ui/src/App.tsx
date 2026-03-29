import { invoke } from "@tauri-apps/api/core";
import { useState } from "react";

function App() {
  const [pingResult, setPingResult] = useState<string>("");

  async function testPing() {
    const result = await invoke<string>("ping");
    setPingResult(result);
  }

  return (
    <div>
      <h1>TVRenamer</h1>
      <p>Scaffold placeholder — file table implemented in UI plan.</p>
      <button onClick={testPing}>Test IPC ping</button>
      {pingResult && <p>IPC response: {pingResult}</p>}
    </div>
  );
}

export default App;
