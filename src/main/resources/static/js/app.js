// --- CONFIGURAÇÕES E ROTAS ---
const API_WIFI = '/api/wifi/authorized';
const API_IMPORT = '/api/wifi/import';
const API_USERS = '/api/users';

// --- NAVEGAÇÃO ENTRE ABAS ---
function showTab(tabId) {
    document.querySelectorAll('.dashboard-tab').forEach(tab => tab.classList.add('d-none'));
    document.getElementById(tabId).classList.remove('d-none');
}

// ==========================================
// MÓDULO 1: REDES WI-FI E CSV
// ==========================================

async function loadNetworks() {
    try {
        const response = await fetch(API_WIFI);
        const networks = await response.json();
        const tbody = document.getElementById('networkTableBody');
        tbody.innerHTML = '';

        if (networks.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">Nenhuma rede Wi-Fi cadastrada.</td></tr>';
            return;
        }

        networks.forEach(net => {
            tbody.innerHTML += `
                <tr>
                    <td class="fw-semibold text-primary"><i class="bi bi-router"></i> ${net.ssid}</td>
                    <td><span class="password-blur" onclick="this.classList.toggle('revealed')" title="Clique para revelar">${net.password}</span></td>
                    <td class="text-secondary">${net.location}</td>
                    <td class="text-end">
                        <button class="btn btn-outline-danger btn-sm" onclick="deleteNetwork(${net.id})"><i class="bi bi-trash"></i></button>
                    </td>
                </tr>
            `;
        });
    } catch (error) {
        console.error("Erro ao carregar redes:", error);
    }
}

async function deleteNetwork(id) {
    if(confirm('Apagar esta rede? As ESP32 não a encontrarão mais.')) {
        await fetch(`${API_WIFI}/${id}`, { method: 'DELETE' });
        loadNetworks();
    }
}

async function uploadCsv() {
    const fileInput = document.getElementById('csvFileInput');
    if (fileInput.files.length === 0) {
        alert("Por favor, selecione um ficheiro CSV primeiro.");
        return;
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    try {
        const response = await fetch(API_IMPORT, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();
        if (response.ok) {
            alert(result.message); // Exibe "Sucesso! X redes importadas."
            loadNetworks(); // Recarrega a tabela imediatamente

            // Fecha o modal do Bootstrap
            const modalEl = document.getElementById('csvModal');
            const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
            modal.hide();
            fileInput.value = ''; // Limpa o input
        } else {
            alert(result.error);
        }
    } catch (error) {
        alert("Erro ao enviar o ficheiro.");
    }
}

// ==========================================
// MÓDULO 2: GESTÃO DE USUÁRIOS (IAM)
// ==========================================

async function loadUsers() {
    try {
        const response = await fetch(API_USERS);
        const users = await response.json();
        const tbody = document.getElementById('usersTableBody');
        tbody.innerHTML = '';

        let pendingCount = 0;

        users.forEach(user => {
            // Conta quantos estão pendentes para o alerta vermelho no menu
            if (user.status === 'PENDING') pendingCount++;

            // Formatação da Data de Validade
            let validadeStr = user.expirationDate ? new Date(user.expirationDate).toLocaleString('pt-BR') : '<span class="badge bg-success">Vitalício</span>';

            // Cor do Status (Badge)
            let statusBadge = '';
            if (user.status === 'APPROVED') statusBadge = '<span class="badge bg-success">Aprovado</span>';
            else if (user.status === 'PENDING') statusBadge = '<span class="badge bg-warning text-dark">Pendente</span>';
            else if (user.status === 'BANNED') statusBadge = '<span class="badge bg-danger">Banido</span>';
            else statusBadge = `<span class="badge bg-secondary">${user.status}</span>`;

            // Botões de Ação Dinâmicos
            let actionButtons = '';
            if (user.status === 'PENDING') {
                actionButtons = `
                    <button class="btn btn-success btn-sm" onclick="openApproveModal(${user.id})"><i class="bi bi-check-lg"></i> Aprovar</button>
                    <button class="btn btn-danger btn-sm" onclick="changeUserStatus(${user.id}, 'REJECTED')"><i class="bi bi-x-lg"></i> Rejeitar</button>
                `;
            } else if (user.status === 'APPROVED') {
                actionButtons = `<button class="btn btn-outline-danger btn-sm" onclick="changeUserStatus(${user.id}, 'BANNED')"><i class="bi bi-slash-circle"></i> Banir</button>`;
            } else if (user.status === 'BANNED' || user.status === 'REJECTED') {
                actionButtons = `<button class="btn btn-outline-success btn-sm" onclick="changeUserStatus(${user.id}, 'APPROVED')"><i class="bi bi-arrow-counterclockwise"></i> Restaurar</button>`;
            }

            tbody.innerHTML += `
                <tr>
                    <td>
                        <div class="fw-bold">${user.name}</div>
                        <div class="text-muted small">${user.email}</div>
                    </td>
                    <td>${user.appClient ? user.appClient.name : '-'}</td>
                    <td>${statusBadge}</td>
                    <td>${validadeStr}</td>
                    <td class="text-end">${actionButtons}</td>
                </tr>
            `;
        });

        // Atualiza a bolinha vermelha no menu
        const badge = document.getElementById('pendingCount');
        badge.innerText = pendingCount;
        badge.style.display = pendingCount > 0 ? 'inline-block' : 'none';

    } catch (error) {
        console.error("Erro ao carregar usuários:", error);
    }
}

// Prepara e abre o modal de aprovação
function openApproveModal(userId) {
    document.getElementById('approveUserId').value = userId;
    document.getElementById('tbUrl').value = '';
    document.getElementById('tbUser').value = '';
    document.getElementById('tbPass').value = '';
    document.getElementById('expirationDate').value = '';

    const modal = new bootstrap.Modal(document.getElementById('approveModal'));
    modal.show();
}

// Dispara a aprovação real com os dados do ThingsBoard
async function confirmApproval() {
    const id = document.getElementById('approveUserId').value;

    const payload = {
        tbUrl: document.getElementById('tbUrl').value,
        tbUsername: document.getElementById('tbUser').value,
        tbPassword: document.getElementById('tbPass').value,
        expirationDate: document.getElementById('expirationDate').value || null
    };

    try {
        const response = await fetch(`${API_USERS}/${id}/approve`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            // Esconde o modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('approveModal'));
            modal.hide();
            // Atualiza a tabela
            loadUsers();
        } else {
            alert("Erro ao aprovar o utilizador.");
        }
    } catch (error) {
        console.error(error);
    }
}

// Muda o status rapidamente (Banir, Restaurar, Rejeitar)
async function changeUserStatus(id, newStatus) {
    if(confirm(`Tem a certeza que deseja mudar o status para ${newStatus}?`)) {
        await fetch(`${API_USERS}/${id}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });
        loadUsers();
    }
}

// --- ARRANQUE INICIAL ---
window.onload = () => {
    loadNetworks();
    loadUsers();
};