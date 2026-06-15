// UI Elements
const elSymbolsList = document.getElementById('symbols-list');
const elSearchInput = document.getElementById('search-input');
const elStatusPanel = document.getElementById('status-panel');
const elActiveSymbolTitle = document.getElementById('active-symbol-title');
const elBadgeDot = document.getElementById('badge-dot');
const elBadgeStatusText = document.getElementById('badge-status-text');
const elTickCounter = document.getElementById('tick-counter');
const elBtnStartSim = document.getElementById('btn-start-sim');
const elBtnStopSim = document.getElementById('btn-stop-sim');
const elPlaceholderPanel = document.getElementById('placeholder-panel');
const elWorkspaceGrid = document.getElementById('workspace-grid');
const elConnectionError = document.getElementById('connection-error');
const elBtnChartLine = document.getElementById('btn-chart-line');
const elBtnChartCandles = document.getElementById('btn-chart-candles');
const elChartContainer = document.getElementById('chart-container');
const elAsksList = document.getElementById('asks-list');
const elBidsList = document.getElementById('bids-list');
const elSpreadValue = document.getElementById('spread-value');

// App State
let selectedSymbol = null;
let eventSource = null;
let symbols = [];
let chart = null;
let lineSeries = null;
let candleSeries = null;
let currentChartMode = 'line'; // 'line' or 'candles'

// Performance Optimization: DOM Caching
let askRows = [];
let bidRows = [];

// Candle Aggregator State (1-second candles)
let currentSecond = 0;
let currentCandle = null;

// Fetch and Render Symbols List (Static, on load)
async function initSymbols() {
  try {
    const res = await fetch('/api/simulator/symbols');
    if (!res.ok) throw new Error('Network error');
    symbols = await res.json();
    renderSymbols();
  } catch (err) {
    elSymbolsList.innerHTML = `<div class="loading-state" style="color: #ef4444;">Failed to load symbols</div>`;
    console.error(err);
  }
}

function renderSymbols() {
  const filter = elSearchInput.value.toLowerCase();
  const filtered = symbols.filter(s => s.toLowerCase().includes(filter));

  if (filtered.length === 0) {
    elSymbolsList.innerHTML = `<div class="loading-state">No instruments found</div>`;
    return;
  }

  elSymbolsList.innerHTML = filtered.map(sym => `
    <div class="symbol-item ${selectedSymbol === sym ? 'active' : ''}" data-symbol="${sym}">
      <span class="symbol-name">${sym}</span>
    </div>
  `).join('');

  // Add click listeners
  document.querySelectorAll('.symbol-item').forEach(item => {
    item.addEventListener('click', () => {
      const sym = item.getAttribute('data-symbol');
      selectSymbol(sym);
    });
  });
}

// Search Input Listener
elSearchInput.addEventListener('input', renderSymbols);

// Select Symbol & Establish SSE connection
function selectSymbol(symbol) {
  if (selectedSymbol === symbol) return;

  selectedSymbol = symbol;
  renderSymbols(); // Update active highlights in sidebar

  // Show Workspace and Header Status
  elPlaceholderPanel.style.display = 'none';
  elWorkspaceGrid.style.display = 'grid';
  elStatusPanel.style.display = 'flex';
  elActiveSymbolTitle.textContent = symbol;

  // Reset/Recreate Charts & Connection
  connectSSE(symbol);
}

// Pre-create 10 static rows for bids and asks to avoid innerHTML parsing lag
function createStaticRows() {
  elAsksList.innerHTML = '';
  elBidsList.innerHTML = '';
  
  askRows = [];
  bidRows = [];

  // Create Asks (rendered top to bottom)
  for (let i = 0; i < 10; i++) {
    const row = document.createElement('div');
    row.className = 'ob-row';
    row.style.display = 'none'; // Hidden initially
    
    const fill = document.createElement('div');
    fill.className = 'depth-fill ask-fill';
    
    const price = document.createElement('span');
    price.className = 'price-label ask-color';
    
    const qty = document.createElement('span');
    qty.className = 'qty-label';
    
    const total = document.createElement('span');
    total.className = 'total-label';
    
    row.appendChild(fill);
    row.appendChild(price);
    row.appendChild(qty);
    row.appendChild(total);
    
    elAsksList.appendChild(row);
    askRows.push({ row, fill, price, qty, total });
  }

  // Create Bids (rendered top to bottom)
  for (let i = 0; i < 10; i++) {
    const row = document.createElement('div');
    row.className = 'ob-row';
    row.style.display = 'none'; // Hidden initially
    
    const fill = document.createElement('div');
    fill.className = 'depth-fill bid-fill';
    
    const price = document.createElement('span');
    price.className = 'price-label bid-color';
    
    const qty = document.createElement('span');
    qty.className = 'qty-label';
    
    const total = document.createElement('span');
    total.className = 'total-label';
    
    row.appendChild(fill);
    row.appendChild(price);
    row.appendChild(qty);
    row.appendChild(total);
    
    elBidsList.appendChild(row);
    bidRows.push({ row, fill, price, qty, total });
  }
}

// Connect to real-time SSE stream
function connectSSE(symbol) {
  // Close previous stream if open
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }

  elConnectionError.style.display = 'none';
  resetChart();
  createStaticRows();

  // Reset candle state
  currentSecond = 0;
  currentCandle = null;

  // Connect to backend stream (which delivers instant trades and periodic depth snapshots)
  eventSource = new EventSource(`/api/simulator/stream?symbol=${symbol}`);

  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);

      if (data.type === 'depth') {
        // Update Simulation Status
        updateStatus(data.running, data.currentTick);
        // Update Order Book Depth
        updateOrderBook(data.bids || [], data.asks || []);
      } 
      else if (data.type === 'trade') {
        // Push raw trade ticks immediately to chart for fluid, lag-free lines and correct OHLC candle calculation
        const price = data.price / 100000000.0;
        const timeSec = Math.floor(data.timestampMs / 1000);
        pushPriceToChart(timeSec, price);
      }
    } catch (err) {
      console.error('Failed to parse SSE payload:', err);
    }
  };

  eventSource.onerror = (err) => {
    console.error('SSE Stream Error:', err);
    elConnectionError.style.display = 'inline-block';
  };
}

// Update simulation status bar (no emojis, professional text indicators)
function updateStatus(running, tick) {
  elTickCounter.textContent = `TICK: ${tick}`;
  
  if (running) {
    elBadgeDot.className = 'status-dot running';
    elBadgeStatusText.textContent = 'ACTIVE';
    elBtnStartSim.disabled = true;
    elBtnStopSim.disabled = false;
  } else {
    elBadgeDot.className = 'status-dot stopped';
    elBadgeStatusText.textContent = 'STOPPED';
    elBtnStartSim.disabled = false;
    elBtnStopSim.disabled = true;
  }
}

// Handle simulation start
elBtnStartSim.addEventListener('click', async () => {
  try {
    const res = await fetch('/api/simulator/start', { method: 'POST' });
    if (!res.ok) throw new Error('Action failed');
    elBadgeDot.className = 'status-dot running';
    elBadgeStatusText.textContent = 'ACTIVE';
    elBtnStartSim.disabled = true;
    elBtnStopSim.disabled = false;
  } catch (err) {
    console.error('Failed to start simulation:', err);
  }
});

// Handle simulation stop
elBtnStopSim.addEventListener('click', async () => {
  try {
    const res = await fetch('/api/simulator/stop', { method: 'POST' });
    if (!res.ok) throw new Error('Action failed');
    elBadgeDot.className = 'status-dot stopped';
    elBadgeStatusText.textContent = 'STOPPED';
    elBtnStartSim.disabled = false;
    elBtnStopSim.disabled = true;
  } catch (err) {
    console.error('Failed to stop simulation:', err);
  }
});

// Update order book rows using pre-cached DOM nodes instead of innerHTML recreation
function updateOrderBook(bids, asks) {
  const maxQty = Math.max(
    ...bids.map(b => b.quantity),
    ...asks.map(a => a.quantity),
    1
  );

  // 1. Render Asks (Sells) - sorted high-to-low (reverse order of asks to place best ask near spread)
  const sortedAsks = [...asks].slice(0, 10).reverse();
  for (let i = 0; i < 10; i++) {
    const rowObj = askRows[i];
    // Adjust data index due to asks reversal
    const dataIndex = i - (10 - sortedAsks.length);

    if (dataIndex >= 0 && dataIndex < sortedAsks.length) {
      const level = sortedAsks[dataIndex];
      const pct = (level.quantity / maxQty) * 100;
      
      rowObj.fill.style.width = `${pct}%`;
      rowObj.price.textContent = (level.price / 100000000.0).toFixed(5);
      rowObj.qty.textContent = level.quantity;
      rowObj.total.textContent = (level.price * level.quantity / 100000000.0).toFixed(2);
      rowObj.row.style.display = 'grid';
    } else {
      rowObj.row.style.display = 'none';
    }
  }

  // 2. Render Spread
  if (bids.length > 0 && asks.length > 0) {
    const spread = (asks[0].price - bids[0].price) / 100000000.0;
    elSpreadValue.textContent = spread.toFixed(5);
  } else {
    elSpreadValue.textContent = '0.00000';
  }

  // 3. Render Bids (Buys)
  const sortedBids = [...bids].slice(0, 10);
  for (let i = 0; i < 10; i++) {
    const rowObj = bidRows[i];
    
    if (i < sortedBids.length) {
      const level = sortedBids[i];
      const pct = (level.quantity / maxQty) * 100;
      
      rowObj.fill.style.width = `${pct}%`;
      rowObj.price.textContent = (level.price / 100000000.0).toFixed(5);
      rowObj.qty.textContent = level.quantity;
      rowObj.total.textContent = (level.price * level.quantity / 100000000.0).toFixed(2);
      rowObj.row.style.display = 'grid';
    } else {
      rowObj.row.style.display = 'none';
    }
  }
}

// Chart Initializer using TradingView Lightweight Charts
function resetChart() {
  elChartContainer.innerHTML = '';

  // Create Chart matching the terminal theme
  chart = LightweightCharts.createChart(elChartContainer, {
    width: elChartContainer.clientWidth,
    height: elChartContainer.clientHeight,
    layout: {
      background: { color: '#111318' },
      textColor: '#7f8c9d',
    },
    grid: {
      vertLines: { color: '#1c1f26' },
      horzLines: { color: '#1c1f26' },
    },
    timeScale: {
      timeVisible: true,
      secondsVisible: true,
      borderColor: '#1c1f26',
    },
    rightPriceScale: {
      borderColor: '#1c1f26',
      autoScale: true,
    }
  });

  // Create Area/Line series (Tick Mode)
  lineSeries = chart.addAreaSeries({
    lineColor: '#2563eb', // Slate blue line
    topColor: 'rgba(37, 99, 235, 0.12)',
    bottomColor: 'rgba(37, 99, 235, 0.00)',
    lineWidth: 2,
    visible: currentChartMode === 'line',
  });

  // Create Candlestick series (Candle Mode)
  candleSeries = chart.addCandlestickSeries({
    upColor: '#10b981',
    downColor: '#ef4444',
    borderUpColor: '#10b981',
    borderDownColor: '#ef4444',
    wickUpColor: '#10b981',
    wickDownColor: '#ef4444',
    visible: currentChartMode === 'candles',
  });

  // Handle Resize
  const handleResize = () => {
    if (chart) {
      chart.applyOptions({
        width: elChartContainer.clientWidth,
        height: elChartContainer.clientHeight,
      });
    }
  };
  window.addEventListener('resize', handleResize);
}

// Push prices to both Tick (line) and Candle series
function pushPriceToChart(timeSec, price) {
  if (!chart) return;

  // A. Update line/area series directly (tick level)
  lineSeries.update({
    time: timeSec,
    value: price
  });

  // B. Aggregate into 1-second candles
  if (currentSecond === 0 || currentSecond !== timeSec) {
    currentSecond = timeSec;
    currentCandle = {
      time: timeSec,
      open: price,
      high: price,
      low: price,
      close: price
    };
    candleSeries.update(currentCandle);
  } else {
    // Update active candle values
    currentCandle.high = Math.max(currentCandle.high, price);
    currentCandle.low = Math.min(currentCandle.low, price);
    currentCandle.close = price;
    candleSeries.update(currentCandle);
  }
}

// Chart Toggle Controls (Line vs Candles)
elBtnChartLine.addEventListener('click', () => {
  if (currentChartMode === 'line') return;
  currentChartMode = 'line';
  
  elBtnChartLine.classList.add('active');
  elBtnChartCandles.classList.remove('active');

  if (lineSeries && candleSeries) {
    lineSeries.applyOptions({ visible: true });
    candleSeries.applyOptions({ visible: false });
  }
});

elBtnChartCandles.addEventListener('click', () => {
  if (currentChartMode === 'candles') return;
  currentChartMode = 'candles';

  elBtnChartCandles.classList.add('active');
  elBtnChartLine.classList.remove('active');

  if (lineSeries && candleSeries) {
    lineSeries.applyOptions({ visible: false });
    candleSeries.applyOptions({ visible: true });
  }
});

// Fetch initial status on load to enable/disable buttons correctly
async function checkInitialStatus() {
  try {
    const res = await fetch('/api/simulator/status');
    if (res.ok) {
      const status = await res.json();
      updateStatus(status.running, status.currentTick);
    }
  } catch (err) {
    console.error('Failed to fetch initial status:', err);
  }
}

// Initialize on page load
initSymbols();
checkInitialStatus();
