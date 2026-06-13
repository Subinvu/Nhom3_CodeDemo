const API_BASE = '/api';

const form = document.querySelector("#order-form");
const productList = document.querySelector("#product-list");
const btnAddProduct = document.querySelector("#btn-add-product");
const grandTotal = document.querySelector("#grandTotal");

const formStatus = document.querySelector("#form-status");
const resultCard = document.querySelector("#result-card");
const resCluster = document.querySelector("#res-cluster");
const resRfm = document.querySelector("#res-rfm");
const submitButton = document.querySelector("#submit-button");
const resetButton = document.querySelector("#reset-button");

const btnSearchCustomer = document.querySelector("#btn-search-customer");
const searchPhoneInput = document.querySelector("#search-phone");
const customerPhoneInput = document.querySelector("#customer-phone");
const customerNameInput = document.querySelector("#customer-name");
const customerEmailInput = document.querySelector("#customer-email");
const aiBadge = document.querySelector("#ai-suggestion-badge");

if (btnSearchCustomer) {
  btnSearchCustomer.addEventListener('click', async () => {
    const phone = searchPhoneInput.value.trim();
    if (!phone) {
      alert("Vui lòng nhập số điện thoại để tra cứu!");
      return;
    }
    
    btnSearchCustomer.textContent = "Đang tìm...";
    btnSearchCustomer.disabled = true;
    
    try {
      const res = await fetch(`${API_BASE}/khachhang/search?q=${phone}&size=1`);
      const data = await res.json();
      
      if (data.content && data.content.length > 0) {
        const kh = data.content[0];
        if (kh.sdt === phone || phone.includes(kh.sdt)) {
            customerNameInput.value = kh.tenKh || '';
            customerEmailInput.value = kh.email || '';
            customerPhoneInput.value = kh.sdt || phone;
            
            aiBadge.style.display = 'block';
            const cn = kh.clusterName || '';
            if (cn.toLowerCase().includes('vip') || cn.toLowerCase().includes('champions')) {
                aiBadge.style.backgroundColor = '#fef3c7';
                aiBadge.style.color = '#92400e';
                aiBadge.style.border = '1px solid #fcd34d';
                aiBadge.innerHTML = '🏆 <b>Khách quen (VIP)</b>: Đề xuất tặng voucher giảm giá 10% để giữ chân khách hàng!';
            } else if (cn.toLowerCase().includes('rời bỏ') || cn.toLowerCase().includes('lost')) {
                aiBadge.style.backgroundColor = '#fee2e2';
                aiBadge.style.color = '#b91c1c';
                aiBadge.style.border = '1px solid #fca5a5';
                aiBadge.innerHTML = '📉 <b>Khách lâu chưa quay lại</b>: Hãy hỏi thăm khách hàng, tư vấn thêm các sản phẩm mới!';
            } else if (cn.toLowerCase().includes('thường xuyên') || cn.toLowerCase().includes('loyal')) {
                aiBadge.style.backgroundColor = '#e0e7ff';
                aiBadge.style.color = '#3730a3';
                aiBadge.style.border = '1px solid #a5b4fc';
                aiBadge.innerHTML = '💎 <b>Khách hàng tiềm năng</b>: Giới thiệu thêm các sản phẩm liên quan để Upsell.';
            } else {
                aiBadge.style.backgroundColor = '#dcfce7';
                aiBadge.style.color = '#166534';
                aiBadge.style.border = '1px solid #86efac';
                aiBadge.innerHTML = `🌱 <b>Khách hàng</b>: ${cn || 'Mới'}. Hãy phục vụ chu đáo!`;
            }
        } else {
            handleNewCustomer();
        }
      } else {
        handleNewCustomer();
      }
    } catch (e) {
      console.error(e);
      alert("Lỗi khi tra cứu khách hàng.");
    } finally {
      btnSearchCustomer.textContent = "🔍 Kiểm tra";
      btnSearchCustomer.disabled = false;
    }
  });
}

function handleNewCustomer() {
    customerNameInput.value = '';
    customerEmailInput.value = '';
    customerPhoneInput.value = searchPhoneInput.value.trim();
    aiBadge.style.display = 'block';
    aiBadge.style.backgroundColor = '#f1f5f9';
    aiBadge.style.color = '#334155';
    aiBadge.style.border = '1px solid #cbd5e1';
    aiBadge.innerHTML = '✨ <b>Khách hàng mới</b>: Vui lòng nhập thông tin Tên và Email để tạo hồ sơ!';
}

let productIndex = 0;

// ── RENDER SẢN PHẨM ĐỘNG ─────────────────────────────────────────

function addProductCard() {
  const article = document.createElement("article");
  article.className = "product-card";
  article.innerHTML = `
    <div class="product-meta" style="display: flex; flex-direction: column; gap: 8px;">
      <input type="text" class="inp-product-name" placeholder="Nhập tên sản phẩm..." required style="height: 40px; padding: 8px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; width: 100%;" />
      <input type="number" class="inp-product-price" placeholder="Đơn giá (£)" required min="0" step="10" style="height: 40px; padding: 8px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; width: 100%;" />
      <button type="button" class="btn-remove-product" style="align-self: flex-start; color: #dc2626; background: none; border: none; cursor: pointer; padding: 0; font-size: 14px; margin-top: 4px; font-weight: 600;">✕ Xóa sản phẩm</button>
    </div>
    <div class="quantity-box">
      <label>Số lượng</label>
      <input
        class="inp-qty"
        type="number"
        min="1"
        step="1"
        value="1"
      />
    </div>
  `;

  productList.appendChild(article);
  productIndex++;

  // Gắn sự kiện tính tổng tiền và xóa
  const qtyInput = article.querySelector('.inp-qty');
  const priceInput = article.querySelector('.inp-product-price');
  const removeBtn = article.querySelector('.btn-remove-product');

  qtyInput.addEventListener('input', updateGrandTotal);
  priceInput.addEventListener('input', updateGrandTotal);

  removeBtn.addEventListener('click', () => {
    article.remove();
    updateGrandTotal();
  });

  // Focus vào ô tên sản phẩm mới
  article.querySelector('.inp-product-name').focus();
}

function updateGrandTotal() {
  let total = 0;
  const cards = productList.querySelectorAll('.product-card');
  cards.forEach(card => {
    const qty = parseInt(card.querySelector('.inp-qty').value) || 0;
    const price = parseFloat(card.querySelector('.inp-product-price').value) || 0;
    total += qty * price;
  });
  grandTotal.textContent = '£' + formatNumber(total);
}

btnAddProduct.addEventListener('click', addProductCard);


// ── SUBMIT & VALIDATION ──────────────────────────────────────────

function getSelectedItems() {
  const items = [];
  const cards = productList.querySelectorAll('.product-card');
  cards.forEach(card => {
    const tenSp = card.querySelector('.inp-product-name').value.trim();
    const donGia = parseFloat(card.querySelector('.inp-product-price').value) || 0;
    const soLuong = parseInt(card.querySelector('.inp-qty').value) || 0;

    if (tenSp && soLuong > 0) {
      items.push({ tenSp, donGia, soLuong });
    }
  });
  return items;
}

function buildPayload() {
  const tenKh = document.querySelector("#customer-name").value.trim();
  const email = document.querySelector("#customer-email").value.trim();
  const sdt = document.querySelector("#customer-phone").value.trim();
  const chiTietList = getSelectedItems();

  return {
    tenKh,
    email,
    sdt,
    chiTietList
  };
}

function setFieldError(element, hasError) {
  element.setAttribute("aria-invalid", hasError ? "true" : "false");
}

function validateForm(payload) {
  const nameInput = document.querySelector("#customer-name");
  const emailInput = document.querySelector("#customer-email");
  const phoneInput = document.querySelector("#customer-phone");
  const phonePattern = /^[0-9]{9,11}$/;

  let isValid = true;

  setFieldError(nameInput, false);
  setFieldError(emailInput, false);
  setFieldError(phoneInput, false);

  if (!payload.tenKh) {
    setFieldError(nameInput, true);
    isValid = false;
  }

  if (!payload.email || !emailInput.checkValidity()) {
    setFieldError(emailInput, true);
    isValid = false;
  }

  if (payload.sdt && !phonePattern.test(payload.sdt)) {
    setFieldError(phoneInput, true);
    isValid = false;
  }

  if (payload.chiTietList.length === 0) {
    isValid = false;
  }

  return isValid;
}

function setStatus(message, type) {
  formStatus.textContent = message;
  formStatus.className = `form-status ${type}`;
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const payload = buildPayload();

  if (!validateForm(payload)) {
    setStatus("Vui lòng điền đủ thông tin khách hàng và đảm bảo có ít nhất 1 sản phẩm hợp lệ.", "error");
    resultCard.style.display = "none";
    return;
  }

  submitButton.disabled = true;
  submitButton.textContent = "Đang gửi...";

  try {
    const response = await fetch(`${API_BASE}/donhang`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || `Request failed with status ${response.status}`);
    }

    setStatus("Đã đặt hàng và phân cụm thành công!", "success");
    
    // Hiển thị kết quả — emoji map theo tên nhãn mới từ rfm_kmeans.py
    resultCard.style.display = "block";
    let emoji = '❓';
    const cn = data.clusterName || '';
    if (cn.includes('VIP') || cn.includes('Champions'))                           emoji = '🏆';
    else if (cn.includes('thường xuyên') || cn.includes('Loyal'))                 emoji = '💎';
    else if (cn.includes('mới') || cn.includes('Cần chú ý') || cn.includes('Phổ Thông')) emoji = '🌱';
    else if (cn.includes('rời bỏ') || cn.includes('Lost'))                        emoji = '📉';

    resCluster.innerHTML = `${emoji} ${cn || 'Chưa phân cụm'}`;
    resCluster.style.color = "#2563eb";
    resRfm.innerHTML = `Mã ĐH: <b>#${data.maDh}</b><br/>Tổng tiền: <b>£${formatNumber(data.tongTienDonHang)}</b><br/>Recency: <b>${data.recency ?? '—'}</b> | Frequency: <b>${data.frequency ?? '—'}</b> | Monetary: <b>£${formatNumber(data.monetary)}</b>`;


    // Cuộn xuống để xem kết quả
    resultCard.scrollIntoView({ behavior: 'smooth', block: 'start' });

  } catch (error) {
    setStatus(error.message || "Lỗi kết nối tới Server.", "error");
    resultCard.style.display = "none";
    console.error(error);
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "Submit";
  }
});

resetButton.addEventListener("click", () => {
  form.reset();
  productList.innerHTML = '';
  addProductCard(); // Thêm lại 1 dòng trống
  updateGrandTotal();
  setStatus("", "");
  resultCard.style.display = "none";
});

function formatNumber(n) {
  if (n === null || n === undefined) return '0';
  return Math.round(n).toLocaleString('vi-VN');
}

// Khởi tạo 1 dòng sản phẩm đầu tiên
addProductCard();
