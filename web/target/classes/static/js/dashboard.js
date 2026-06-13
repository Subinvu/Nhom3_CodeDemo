/**
 * dashboard.js — Logic trang Admin Dashboard
 * Fetch dữ liệu từ API, render Chart.js charts, Data Table với search/filter/pagination
 */

const API = '/api/dashboard';

// ── CLUSTER CONFIG ──────────────────────────────────────────────
// Tên nhãn khớp với label_map từ rfm_kmeans.py (Dynamic Labeling)
const CLUSTER_CONFIG = {
    'VIP':                                       { color: '#00d4aa', bg: 'rgba(0,212,170,0.15)',   emoji: '🏆', cssClass: 'badge-champions' },
    'Khách hàng thường xuyên / Tiềm năng':       { color: '#667eea', bg: 'rgba(102,126,234,0.15)', emoji: '💎', cssClass: 'badge-loyal'     },
    'Khách hàng mới / Cần chú ý':               { color: '#38bdf8', bg: 'rgba(56,189,248,0.15)',  emoji: '🌱', cssClass: 'badge-atrisk'   },
    'Khách hàng rời bỏ / Nguy cơ cao':          { color: '#e74c3c', bg: 'rgba(231,76,60,0.15)',   emoji: '📉', cssClass: 'badge-lost'     },
    // --- Fallback aliases (tên cũ) để dashboard không bị trắng khi DB còn tên cũ ---
    'Champions':                                 { color: '#00d4aa', bg: 'rgba(0,212,170,0.15)',   emoji: '🏆', cssClass: 'badge-champions' },
    'Loyal Customers':                           { color: '#667eea', bg: 'rgba(102,126,234,0.15)', emoji: '💎', cssClass: 'badge-loyal'     },
    'Khách Phổ Thông/Tiềm Năng':                { color: '#38bdf8', bg: 'rgba(56,189,248,0.15)',  emoji: '🌱', cssClass: 'badge-atrisk'   },
    'Lost':                                      { color: '#e74c3c', bg: 'rgba(231,76,60,0.15)',   emoji: '📉', cssClass: 'badge-lost'     },
};

function getClusterConfig(name) {
    if (!name) return { color: '#9090b0', bg: 'rgba(144,144,176,0.15)', emoji: '❓', cssClass: 'badge-unknown' };
    for (const [key, val] of Object.entries(CLUSTER_CONFIG)) {
        if (name.includes(key)) return val;
    }
    return { color: '#9090b0', bg: 'rgba(144,144,176,0.15)', emoji: '❓', cssClass: 'badge-unknown' };
}

// ── CHART.JS GLOBAL DEFAULTS ────────────────────────────────────
Chart.defaults.color              = '#9090b0';
Chart.defaults.borderColor        = 'rgba(255,255,255,0.07)';
Chart.defaults.font.family        = 'Inter, sans-serif';
Chart.defaults.plugins.legend.display = false;

// ── STATE ───────────────────────────────────────────────────────
let pieChartInst     = null;
let scatterChartInst = null;
let barChartInst     = null;
let barData          = [];
let activeMetric     = 'R';

let allCustomers     = [];  // Full list for client-side table
let filteredCusts    = [];
let currentPage      = 0;
let pageSize         = 10;
let sortCol          = 'maKh';
let sortAsc          = true;
let searchQ          = '';
let filterCluster    = '';

// ── INIT ────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    loadAll();

    document.getElementById('btnRefresh')?.addEventListener('click', loadAll);
    document.getElementById('btnSyncClusters')?.addEventListener('click', syncClusters);

    // Modal Events
    document.getElementById('btnCloseModal')?.addEventListener('click', closeEditModal);
    document.getElementById('btnCancelEdit')?.addEventListener('click', closeEditModal);
    document.getElementById('btnSaveEdit')?.addEventListener('click', saveEditCustomer);

    // Search & filter
    let searchTimer;
    document.getElementById('searchInput')?.addEventListener('input', e => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => {
            searchQ = e.target.value.trim().toLowerCase();
            currentPage = 0;
            applyFilters();
        }, 300);
    });
    document.getElementById('filterCluster')?.addEventListener('change', e => {
        filterCluster = e.target.value;
        currentPage = 0;
        applyFilters();
    });

    // Pagination
    document.getElementById('btnPrevPage')?.addEventListener('click', () => {
        if (currentPage > 0) { currentPage--; renderTable(); }
    });
    document.getElementById('btnNextPage')?.addEventListener('click', () => {
        const maxPage = Math.ceil(filteredCusts.length / pageSize) - 1;
        if (currentPage < maxPage) { currentPage++; renderTable(); }
    });
    document.getElementById('pageSize')?.addEventListener('change', e => {
        pageSize = parseInt(e.target.value);
        currentPage = 0;
        renderTable();
    });

    // Sort
    document.querySelectorAll('.data-table th.sortable').forEach(th => {
        th.addEventListener('click', () => {
            const col = th.dataset.col;
            if (sortCol === col) sortAsc = !sortAsc;
            else { sortCol = col; sortAsc = true; }
            currentPage = 0;
            applyFilters();
        });
    });

    // Bar metric toggle
    document.querySelectorAll('.toggle-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.toggle-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            activeMetric = btn.dataset.metric;
            updateBarChart();
        });
    });
});

// ── LOAD ALL DATA ───────────────────────────────────────────────
async function loadAll() {
    try {
        const [stats, clusters, scatter, bar, customers] = await Promise.all([
            fetchJSON(`${API}/stats`),
            fetchJSON(`${API}/clusters`),
            fetchJSON(`${API}/rfm-scatter`),
            fetchJSON(`${API}/rfm-bar`),
            fetchJSON('/api/khachhang?size=1000&sort=maKh')
        ]);

        renderStatCards(stats);
        renderPieChart(clusters);
        renderScatterChart(scatter);
        barData = bar;
        renderBarChart(bar);

        // Table data from customers endpoint
        allCustomers   = customers.content || [];
        filteredCusts  = [...allCustomers];
        currentPage    = 0;
        applyFilters();

    } catch (err) {
        console.error('Lỗi load dashboard:', err);
    }
}

// ── SYNC CLUSTERS ───────────────────────────────────────────────
async function syncClusters() {
    if (!confirm('Bạn có chắc muốn đồng bộ phân cụm cho các khách hàng còn thiếu không? Việc này có thể mất vài giây.')) return;
    try {
        const btn = document.getElementById('btnSyncClusters');
        btn.textContent = '⏳ Đang đồng bộ...';
        btn.disabled = true;

        const res = await fetch(`/api/khachhang/sync-clusters`, { method: 'POST' });
        const data = await res.json();
        alert(data.message || 'Đồng bộ hoàn tất');
        loadAll(); // Reload lại toàn bộ dữ liệu
    } catch (err) {
        console.error(err);
        alert('Có lỗi xảy ra khi đồng bộ.');
    } finally {
        const btn = document.getElementById('btnSyncClusters');
        btn.textContent = '🧠 Phân cụm tất cả';
        btn.disabled = false;
    }
}

async function fetchJSON(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status} — ${url}`);
    return res.json();
}

// ── STAT CARDS ──────────────────────────────────────────────────
function renderStatCards(stats) {
    if (!document.getElementById('statTongKH')) return;
    animateNumber('statTongKH',    stats.tongKhachHang,   0);
    animateNumber('statTongDH',    stats.tongDonHang,     0);
    animateNumber('statDoanhThu',  stats.tongDoanhThu,    0, true);
    animateNumber('statSoCum',     stats.soCum,           0);
}

function animateNumber(id, target, start, isCurrency = false) {
    const el    = document.getElementById(id);
    const dur   = 800;
    const steps = 40;
    const step  = (target - start) / steps;
    let cur     = start;
    let count   = 0;
    const iv = setInterval(() => {
        cur += step; count++;
        if (count >= steps) { cur = target; clearInterval(iv); }
        el.textContent = isCurrency ? formatCurrency(cur) : Math.round(cur).toLocaleString('vi-VN');
    }, dur / steps);
}

// ── PIE CHART ───────────────────────────────────────────────────
function renderPieChart(clusters) {
    if (!document.getElementById('pieChart')) return;
    const labels = clusters.map(c => c.cluster_name || 'N/A');
    const values = clusters.map(c => c.so_luong);
    const colors = clusters.map(c => getClusterConfig(c.cluster_name).color);
    const bgs    = clusters.map(c => getClusterConfig(c.cluster_name).bg);

    if (pieChartInst) pieChartInst.destroy();

    const ctx = document.getElementById('pieChart').getContext('2d');
    pieChartInst = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels,
            datasets: [{
                data: values,
                backgroundColor: bgs,
                borderColor:     colors,
                borderWidth:     2,
                hoverOffset:     8
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                tooltip: {
                    callbacks: {
                        label: ctx => ` ${ctx.label}: ${ctx.parsed} KH (${clusters[ctx.dataIndex].ty_le}%)`
                    }
                }
            },
            animation: { animateRotate: true, duration: 800 }
        }
    });

    // Custom legend
    const legend = document.getElementById('pieLegend');
    legend.innerHTML = clusters.map((c, i) => {
        const cfg = getClusterConfig(c.cluster_name);
        return `<div class="legend-item">
            <span class="legend-dot" style="background:${cfg.color}"></span>
            ${cfg.emoji} ${c.cluster_name}: <strong>${c.so_luong}</strong> (${c.ty_le}%)
        </div>`;
    }).join('');
}

// ── SCATTER CHART ───────────────────────────────────────────────
function renderScatterChart(data) {
    if (!document.getElementById('scatterChart')) return;
    // Group by cluster
    const grouped = {};
    data.forEach(d => {
        const name = d.cluster_name || 'Unknown';
        if (!grouped[name]) grouped[name] = [];
        grouped[name].push({ x: d.frequency, y: d.monetary, label: d.ten_kh });
    });

    const datasets = Object.entries(grouped).map(([name, points]) => {
        const cfg = getClusterConfig(name);
        return {
            label: name,
            data: points,
            backgroundColor: cfg.bg,
            borderColor:     cfg.color,
            borderWidth:     1.5,
            pointRadius:     5,
            pointHoverRadius:8
        };
    });

    if (scatterChartInst) scatterChartInst.destroy();

    const ctx = document.getElementById('scatterChart').getContext('2d');
    scatterChartInst = new Chart(ctx, {
        type: 'scatter',
        data: { datasets },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: {
                legend: { display: true, position: 'top',
                    labels: { color: '#9090b0', boxWidth: 12, padding: 16, font: { size: 11 } }
                },
                tooltip: {
                    callbacks: {
                        label: ctx => {
                            const p = ctx.raw;
                            return ` ${p.label || ''} — F:${p.x} | M:${formatCurrency(p.y)}`;
                        }
                    }
                }
            },
            scales: {
                x: { title: { display: true, text: 'Frequency (lần mua)', color: '#9090b0' },
                     grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { title: { display: true, text: 'Monetary (£)', color: '#9090b0' },
                     grid: { color: 'rgba(255,255,255,0.05)' },
                     ticks: { callback: v => formatCurrencyShort(v) } }
            }
        }
    });
}

// ── BAR CHART ───────────────────────────────────────────────────
function renderBarChart(data) {
    if (!document.getElementById('barChart')) return;
    const labels = data.map(d => d.cluster_name || `Cluster ${d.cluster_id}`);
    const getValues = metric => {
        if (metric === 'R') return data.map(d => d.avg_recency);
        if (metric === 'F') return data.map(d => d.avg_frequency);
        return data.map(d => d.avg_monetary);
    };
    const colors = data.map(d => getClusterConfig(d.cluster_name).color);
    const bgs    = data.map(d => getClusterConfig(d.cluster_name).bg);

    if (barChartInst) barChartInst.destroy();

    const ctx = document.getElementById('barChart').getContext('2d');
    barChartInst = new Chart(ctx, {
        type: 'bar',
        data: {
            labels,
            datasets: [{
                label: getMetricLabel(activeMetric),
                data: getValues(activeMetric),
                backgroundColor: bgs,
                borderColor:     colors,
                borderWidth:     1.5,
                borderRadius:    6
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { grid: { color: 'rgba(255,255,255,0.05)' },
                     ticks: { callback: v => activeMetric === 'M' ? formatCurrencyShort(v) : v } }
            },
            animation: { duration: 500 }
        }
    });
}

function updateBarChart() {
    if (!barData.length || !barChartInst) return;
    const getV = m => barData.map(d => m === 'R' ? d.avg_recency : m === 'F' ? d.avg_frequency : d.avg_monetary);
    barChartInst.data.datasets[0].data  = getV(activeMetric);
    barChartInst.data.datasets[0].label = getMetricLabel(activeMetric);
    barChartInst.options.scales.y.ticks.callback = v => activeMetric === 'M' ? formatCurrencyShort(v) : v;
    barChartInst.update('active');
}

function getMetricLabel(m) {
    if (m === 'R') return 'Recency trung bình (ngày)';
    if (m === 'F') return 'Frequency trung bình (lần)';
    return 'Monetary trung bình (£)';
}

// ── DATA TABLE ──────────────────────────────────────────────────
function applyFilters() {
    filteredCusts = allCustomers.filter(kh => {
        const matchSearch = !searchQ ||
            (kh.tenKh?.toLowerCase().includes(searchQ)) ||
            (kh.email?.toLowerCase().includes(searchQ));
        const matchCluster = !filterCluster || (kh.clusterName === filterCluster);
        return matchSearch && matchCluster;
    });

    // Sort
    filteredCusts.sort((a, b) => {
        let va = a[sortCol], vb = b[sortCol];
        if (typeof va === 'string') va = va?.toLowerCase();
        if (typeof vb === 'string') vb = vb?.toLowerCase();
        if (va == null) return 1; if (vb == null) return -1;
        if (va < vb) return sortAsc ? -1 : 1;
        if (va > vb) return sortAsc ?  1 : -1;
        return 0;
    });

    renderTable();
}

function renderTable() {
    const tbody   = document.getElementById('tableBody');
    if (!tbody) return;
    const total   = filteredCusts.length;
    const maxPage = Math.max(0, Math.ceil(total / pageSize) - 1);
    if (currentPage > maxPage) currentPage = maxPage;

    const start = currentPage * pageSize;
    const slice = filteredCusts.slice(start, start + pageSize);

    if (slice.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="table-loading">📭 Không có dữ liệu</td></tr>`;
    } else {
        tbody.innerHTML = slice.map(kh => {
            const cfg = getClusterConfig(kh.clusterName);
            return `<tr>
                <td>${kh.maKh}</td>
                <td><strong>${escHtml(kh.tenKh)}</strong>
                    ${kh.email ? `<br/><small style="color:var(--text-muted)">${escHtml(kh.email)}</small>` : ''}
                </td>
                <td>${kh.recency ?? 0}</td>
                <td>${kh.frequency ?? 0}</td>
                <td>${formatCurrency(kh.monetary)}</td>
                <td>
                    ${kh.clusterName
                        ? `<span class="badge-cluster ${cfg.cssClass}">${cfg.emoji} ${escHtml(kh.clusterName)}</span>`
                        : `<span class="badge-cluster badge-unknown">❓ Chưa phân cụm</span>`
                    }
                </td>
                <td>
                    <button class="action-btn" onclick="openEditModal(${kh.maKh})" title="Sửa">✏️</button>
                    <button class="action-btn" onclick="deleteCustomer(${kh.maKh})" title="Xóa">🗑️</button>
                </td>
            </tr>`;
        }).join('');
    }

    // Pagination info
    document.getElementById('pageInfo').textContent    = `Trang ${currentPage + 1} / ${Math.max(1, maxPage + 1)}`;
    document.getElementById('pageTotal').textContent   = `dòng / tổng ${total}`;
    document.getElementById('btnPrevPage').disabled    = currentPage <= 0;
    document.getElementById('btnNextPage').disabled    = currentPage >= maxPage;

    // Sort icons
    document.querySelectorAll('.data-table th.sortable').forEach(th => {
        const icon = th.querySelector('.sort-icon');
        if (th.dataset.col === sortCol) {
            icon.textContent = sortAsc ? '↑' : '↓';
            icon.style.color = '#667eea';
        } else {
            icon.textContent = '↕';
            icon.style.color = '';
        }
    });
}

// ── CRUD ACTIONS ────────────────────────────────────────────────
function openEditModal(maKh) {
    const kh = allCustomers.find(k => k.maKh === maKh);
    if (!kh) return;
    document.getElementById('editMaKh').value = kh.maKh;
    document.getElementById('editTenKh').value = kh.tenKh || '';
    document.getElementById('editEmail').value = kh.email || '';
    document.getElementById('editSdt').value = kh.sdt || '';
    document.getElementById('editModal').style.display = 'flex';
}

function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
}

async function saveEditCustomer() {
    const maKh = document.getElementById('editMaKh').value;
    const email = document.getElementById('editEmail').value.trim();
    if (email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            alert('Vui lòng nhập đúng định dạng email (VD: abc@gmail.com)');
            document.getElementById('editEmail').focus();
            return;
        }
    }

    const payload = {
        tenKh: document.getElementById('editTenKh').value.trim(),
        email: email,
        sdt: document.getElementById('editSdt').value.trim()
    };
    try {
        const res = await fetch(`/api/khachhang/${maKh}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (res.ok) {
            closeEditModal();
            loadAll();
        } else alert('Sửa thất bại!');
    } catch (e) {
        console.error(e);
        alert('Lỗi kết nối!');
    }
}

async function deleteCustomer(maKh) {
    if (!confirm('Bạn có chắc chắn muốn xóa khách hàng này cùng toàn bộ lịch sử đơn hàng của họ không?')) return;
    try {
        const res = await fetch(`/api/khachhang/${maKh}`, { method: 'DELETE' });
        if (res.ok) loadAll();
        else alert('Xóa thất bại!');
    } catch (e) {
        console.error(e);
        alert('Lỗi kết nối!');
    }
}

// ── UTILS ───────────────────────────────────────────────────────
function formatCurrency(v) {
    if (v == null) return '£0';
    return '£' + Math.round(v).toLocaleString('vi-VN');
}
function formatCurrencyShort(v) {
    if (v >= 1_000_000_000) return (v/1_000_000_000).toFixed(1) + 'T';
    if (v >= 1_000_000)     return (v/1_000_000).toFixed(1) + 'M';
    if (v >= 1_000)         return (v/1_000).toFixed(0) + 'K';
    return v;
}
function escHtml(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

// ── CUSTOMER 360 PROFILE ─────────────────────────────────────────
let profPieChartInst = null;
let profScatterChartInst = null;

document.getElementById('btnCloseProfile').addEventListener('click', () => {
    document.getElementById('profileModal').style.display = 'none';
});

// Global Search
const globalSearchInput = document.getElementById('globalSearchInput');
const globalSearchDropdown = document.getElementById('globalSearchDropdown');

if (globalSearchInput && globalSearchDropdown) {
    function showGlobalSearchDropdown() {
        const q = globalSearchInput.value.trim().toLowerCase();
        let matches = [];
        
        if (!q) {
            // Show recent/top 8 customers if empty
            matches = allCustomers.slice(0, 8);
        } else {
            matches = allCustomers.filter(kh => 
                (kh.tenKh && kh.tenKh.toLowerCase().includes(q)) || 
                (kh.email && kh.email.toLowerCase().includes(q)) ||
                (kh.maKh && kh.maKh.toString() === q)
            ).slice(0, 8);
        }

        if (matches.length === 0) {
            globalSearchDropdown.innerHTML = '<div class="search-dropdown-item" style="cursor:default;color:#868e96">Không tìm thấy khách hàng.</div>';
        } else {
            const titleHtml = !q ? '<div style="padding: 8px 16px; font-size: 0.8rem; font-weight: bold; color: var(--text-muted); border-bottom: 1px solid var(--border);">Gợi ý Khách hàng</div>' : '';
            globalSearchDropdown.innerHTML = titleHtml + matches.map(kh => `
                <div class="search-dropdown-item" onclick="openProfileModal(${kh.maKh})">
                    <span class="sd-name">${escHtml(kh.tenKh)}</span>
                    <span class="sd-email">${escHtml(kh.email || 'No email')} - Cụm: ${escHtml(kh.clusterName || 'Chưa phân')}</span>
                </div>
            `).join('');
        }
        globalSearchDropdown.style.display = 'block';
    }

    globalSearchInput.addEventListener('input', showGlobalSearchDropdown);
    globalSearchInput.addEventListener('focus', showGlobalSearchDropdown);
    globalSearchInput.addEventListener('click', showGlobalSearchDropdown);

    // Hide dropdown when clicking outside
    document.addEventListener('click', e => {
        if (!e.target.closest('.global-search-wrapper')) {
            globalSearchDropdown.style.display = 'none';
        }
    });
}

async function openProfileModal(maKh) {
    globalSearchDropdown.style.display = 'none';
    globalSearchInput.value = '';
    
    const kh = allCustomers.find(k => k.maKh === maKh);
    if (!kh) return;

    // Info
    document.getElementById('profName').textContent = kh.tenKh || 'N/A';
    document.getElementById('profEmail').textContent = kh.email || 'N/A';
    document.getElementById('profPhone').textContent = kh.sdt || 'N/A';

    const cfg = getClusterConfig(kh.clusterName);
    document.getElementById('profClusterBadge').className = `badge-cluster ${cfg.cssClass}`;
    document.getElementById('profClusterBadge').innerHTML = kh.clusterName ? `${cfg.emoji} ${kh.clusterName}` : '❓ Chưa phân cụm';

    // RFM
    document.getElementById('profR').textContent = kh.recency ?? 0;
    document.getElementById('profF').textContent = kh.frequency ?? 0;
    document.getElementById('profM').textContent = formatCurrency(kh.monetary);

    // Fetch Orders
    document.getElementById('profOrderBody').innerHTML = '<tr><td colspan="2" style="text-align: center;">⏳ Đang tải...</td></tr>';
    document.getElementById('profileModal').style.display = 'flex';

    try {
        const res = await fetch(`/api/donhang/khachhang/${maKh}`);
        if (!res.ok) throw new Error('API Error');
        const orders = await res.json();
        
        if (orders.length === 0) {
            document.getElementById('profOrderBody').innerHTML = '<tr><td colspan="2" style="text-align: center;">📭 Không có lịch sử mua hàng</td></tr>';
        } else {
            document.getElementById('profOrderBody').innerHTML = orders.map(o => `
                <tr>
                    <td>#${o.maDh}</td>
                    <td style="font-weight:600;color:var(--cl-champions)">${formatCurrency(o.tongTien)}</td>
                </tr>
            `).join('');
        }
    } catch (err) {
        document.getElementById('profOrderBody').innerHTML = '<tr><td colspan="2" style="text-align: center; color: red;">Lỗi tải dữ liệu</td></tr>';
    }

    // Render Charts
    renderProfPieChart(kh);
    renderProfScatterChart(kh);
}

async function renderProfPieChart(kh) {
    try {
        const res = await fetch(`${API}/clusters`);
        const clusters = await res.json();
        
        const labels = clusters.map(c => c.cluster_name || 'N/A');
        const values = clusters.map(c => c.so_luong);
        
        // Highlight customer's cluster
        const bgs = clusters.map(c => {
            const config = getClusterConfig(c.cluster_name);
            return c.cluster_name === kh.clusterName ? config.color : config.bg;
        });
        const borderColors = clusters.map(c => getClusterConfig(c.cluster_name).color);

        if (profPieChartInst) profPieChartInst.destroy();
        const ctx = document.getElementById('profPieChart').getContext('2d');
        profPieChartInst = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    data: values,
                    backgroundColor: bgs,
                    borderColor: borderColors,
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                cutout: '70%'
            }
        });
    } catch(e) {
        console.error('Error rendering prof pie chart', e);
    }
}

function renderProfScatterChart(kh) {
    const dataPoints = allCustomers.map(c => ({
        x: c.frequency || 0,
        y: c.monetary || 0
    }));

    const custPoint = {
        x: kh.frequency || 0,
        y: kh.monetary || 0
    };

    if (profScatterChartInst) profScatterChartInst.destroy();
    const ctx = document.getElementById('profScatterChart').getContext('2d');
    
    profScatterChartInst = new Chart(ctx, {
        type: 'scatter',
        data: {
            datasets: [
                {
                    label: 'Khách hàng khác',
                    data: dataPoints,
                    backgroundColor: 'rgba(144,144,176,0.15)',
                    borderColor: 'rgba(144,144,176,0.1)',
                    pointRadius: 2
                },
                {
                    label: kh.tenKh,
                    data: [custPoint],
                    backgroundColor: '#e74c3c',
                    borderColor: '#fff',
                    borderWidth: 2,
                    pointRadius: 8,
                    pointHoverRadius: 10,
                    pointStyle: 'star'
                }
            ]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: ctx => {
                            if (ctx.datasetIndex === 1) return ` Bạn: F:${ctx.raw.x} | M:${formatCurrency(ctx.raw.y)}`;
                            return ` Khách khác: F:${ctx.raw.x} | M:${formatCurrency(ctx.raw.y)}`;
                        }
                    }
                }
            },
            scales: {
                x: { display: false },
                y: { display: false }
            }
        }
    });
}
