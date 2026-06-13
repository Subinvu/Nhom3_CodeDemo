/**
 * JS xử lý cho trang Danh Sách Đơn Hàng (orders.html)
 */

let allOrders = [];
let filteredOrders = [];

// Trạng thái phân trang & sắp xếp
let currentPage = 0;
let pageSize = 10;
let sortCol = 'ngayDat';
let sortAsc = false;

document.addEventListener('DOMContentLoaded', () => {
    initEvents();
    fetchOrders();
});

function initEvents() {
    // Buttons
    document.getElementById('btnRefresh')?.addEventListener('click', fetchOrders);
    
    // Pagination
    document.getElementById('btnPrevPage')?.addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            renderTable();
        }
    });
    document.getElementById('btnNextPage')?.addEventListener('click', () => {
        currentPage++;
        renderTable();
    });
    document.getElementById('pageSize')?.addEventListener('change', e => {
        pageSize = parseInt(e.target.value);
        currentPage = 0;
        renderTable();
    });

    // Search
    document.getElementById('searchInput')?.addEventListener('input', () => {
        currentPage = 0;
        applyFilters();
    });

    // Sort headers
    document.querySelectorAll('.data-table th.sortable').forEach(th => {
        th.addEventListener('click', () => {
            const col = th.dataset.col;
            if (sortCol === col) {
                sortAsc = !sortAsc;
            } else {
                sortCol = col;
                sortAsc = true;
            }
            applyFilters();
        });
    });

    // Modal Details
    document.getElementById('btnCloseModal')?.addEventListener('click', closeDetailModal);
    document.getElementById('btnCancelDetail')?.addEventListener('click', closeDetailModal);
}

async function fetchOrders() {
    try {
        const tbody = document.getElementById('tableBody');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="5" class="table-loading">⏳ Đang tải dữ liệu...</td></tr>`;
        }

        // Lấy danh sách 10000 đơn hàng gần nhất (client-side pagination)
        const res = await fetch('/api/donhang/all?size=10000&sort=ngayDat,desc');
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        
        const data = await res.json();
        allOrders = data.content || [];
        applyFilters();
    } catch (error) {
        console.error("Lỗi khi fetch đơn hàng:", error);
        alert("Không thể tải danh sách đơn hàng.");
    }
}

function applyFilters() {
    const q = (document.getElementById('searchInput')?.value || '').trim().toLowerCase();

    // Lọc
    filteredOrders = allOrders.filter(o => {
        const maDhStr = (o.maDh || '').toString();
        const tenKhStr = (o.tenKh || '').toLowerCase();
        
        const matchSearch = !q || maDhStr.includes(q) || tenKhStr.includes(q);
        return matchSearch;
    });

    // Sắp xếp
    filteredOrders.sort((a, b) => {
        let valA, valB;
        switch (sortCol) {
            case 'maDh': 
                valA = a.maDh; valB = b.maDh; 
                break;
            case 'tenKh': 
                valA = a.tenKh || ''; valB = b.tenKh || ''; 
                break;
            case 'ngayDat': 
                valA = new Date(a.ngayDat).getTime(); valB = new Date(b.ngayDat).getTime(); 
                break;
            case 'tongTien': 
                valA = a.tongTien; valB = b.tongTien; 
                break;
            default: return 0;
        }

        if (valA < valB) return sortAsc ? -1 : 1;
        if (valA > valB) return sortAsc ? 1 : -1;
        return 0;
    });

    renderTable();
}

function renderTable() {
    const tbody = document.getElementById('tableBody');
    if (!tbody) return;
    
    const total = filteredOrders.length;
    const maxPage = Math.max(0, Math.ceil(total / pageSize) - 1);
    if (currentPage > maxPage) currentPage = maxPage;

    const start = currentPage * pageSize;
    const slice = filteredOrders.slice(start, start + pageSize);

    if (slice.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="table-loading">📭 Không có dữ liệu</td></tr>`;
    } else {
        tbody.innerHTML = slice.map(o => {
            return `<tr style="cursor: pointer;" onclick="openDetailModal(${o.maDh})">
                <td><strong>#${o.maDh}</strong></td>
                <td>${escHtml(o.tenKh || 'Khách Vãng Lai')}</td>
                <td>${formatDate(o.ngayDat)}</td>
                <td style="color: var(--primary); font-weight: bold;">${formatCurrency(o.tongTien)}</td>
                <td>
                    <button class="action-btn" title="Xem chi tiết">👁️ Xem</button>
                </td>
            </tr>`;
        }).join('');
    }

    // Phân trang
    if (document.getElementById('pageInfo')) {
        document.getElementById('pageInfo').textContent = `Trang ${currentPage + 1} / ${Math.max(1, maxPage + 1)}`;
        document.getElementById('pageTotal').textContent = `dòng / tổng ${total}`;
        document.getElementById('btnPrevPage').disabled = currentPage <= 0;
        document.getElementById('btnNextPage').disabled = currentPage >= maxPage;
    }

    // Icons
    document.querySelectorAll('.data-table th.sortable').forEach(th => {
        const icon = th.querySelector('.sort-icon');
        if (icon) {
            if (th.dataset.col === sortCol) {
                icon.textContent = sortAsc ? '↑' : '↓';
                icon.style.color = '#667eea';
            } else {
                icon.textContent = '↕';
                icon.style.color = '';
            }
        }
    });
}

async function openDetailModal(maDh) {
    document.getElementById('orderDetailModal').style.display = 'flex';
    document.getElementById('modalMaDh').textContent = maDh;
    document.getElementById('modalTenKh').textContent = 'Đang tải...';
    document.getElementById('modalNgayDat').textContent = 'Đang tải...';
    document.getElementById('modalTongTien').textContent = '';
    
    const tbody = document.getElementById('modalProductsBody');
    tbody.innerHTML = `<tr><td colspan="4" style="text-align: center;">⏳ Đang tải...</td></tr>`;

    try {
        const res = await fetch(`/api/donhang/details/${maDh}`);
        if (!res.ok) throw new Error("Network response was not ok");
        const order = await res.json();

        document.getElementById('modalTenKh').textContent = order.tenKh || 'Khách Vãng Lai';
        document.getElementById('modalNgayDat').textContent = `Ngày đặt: ${formatDate(order.ngayDat)}`;
        document.getElementById('modalTongTien').textContent = `Tổng: ${formatCurrency(order.tongTien)}`;

        if (order.chiTietList && order.chiTietList.length > 0) {
            tbody.innerHTML = order.chiTietList.map(ct => {
                return `<tr>
                    <td>${escHtml(ct.tenSp)}</td>
                    <td style="text-align: center;">${ct.soLuong}</td>
                    <td style="text-align: right;">${formatCurrency(ct.donGia)}</td>
                    <td style="text-align: right; font-weight: bold;">${formatCurrency(ct.thanhTien)}</td>
                </tr>`;
            }).join('');
        } else {
            tbody.innerHTML = `<tr><td colspan="4" style="text-align: center;">Không có sản phẩm nào.</td></tr>`;
        }
    } catch (error) {
        console.error("Lỗi khi lấy chi tiết đơn hàng:", error);
        tbody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: red;">Lỗi tải dữ liệu.</td></tr>`;
    }
}

function closeDetailModal() {
    document.getElementById('orderDetailModal').style.display = 'none';
}

function formatCurrency(val) {
    if (val == null) return '£0.00';
    return '£' + parseFloat(val).toLocaleString('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(isoStr) {
    if (!isoStr) return '';
    const d = new Date(isoStr);
    return d.toLocaleString('vi-VN');
}

function escHtml(s) {
    if (!s) return '';
    return s.toString().replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
