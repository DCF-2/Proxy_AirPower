const API_URL = '/api/wifi/authorized';

// Função para formatar as linhas da tabela
function renderTable(networks) {
    const tbody = document.getElementById('networkTableBody');
    const emptyState = document.getElementById('emptyState');

    tbody.innerHTML = '';

    if (networks.length === 0) {
        emptyState.classList.remove('d-none');
        return;
    } else {
        emptyState.classList.add('d-none');
    }

    networks.forEach(net => {
        // Criamos o HTML da linha
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="fw-semibold text-primary"><i class="bi bi-router"></i> ${net.ssid}</td>
            <td>
                <span class="password-blur" onclick="this.classList.toggle('revealed')" title="Clique para ver a senha">
                    ${net.password}
                </span>
            </td>
            <td class="text-secondary">${net.location}</td>
            <td class="text-end">
                <button class="btn btn-danger btn-sm" onclick="deleteNetwork(${net.id})">
                    <i class="bi bi-trash3"></i> Apagar
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Buscar redes no servidor
async function loadNetworks() {
    try {
        const response = await fetch(API_URL);
        if (!response.ok) throw new Error('Erro ao buscar redes');
        const networks = await response.json();
        renderTable(networks);
    } catch (error) {
        console.error(error);
        alert('Erro de conexão com a API.');
    }
}

// Interceptar o envio do formulário para salvar
document.getElementById('wifiForm').addEventListener('submit', async (e) => {
    e.preventDefault(); // Evita que a página recarregue

    const submitBtn = e.target.querySelector('button');
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> A Guardar...';
    submitBtn.disabled = true;

    const newNetwork = {
        ssid: document.getElementById('ssid').value,
        password: document.getElementById('password').value,
        location: document.getElementById('description').value
    };

    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newNetwork)
        });

        if (response.ok) {
            document.getElementById('wifiForm').reset();
            await loadNetworks(); // Recarrega a tabela imediatamente
        } else {
            alert('Falha ao registar a rede no servidor.');
        }
    } catch (error) {
        console.error(error);
    } finally {
        submitBtn.innerHTML = '<i class="bi bi-save"></i> Registar Rede';
        submitBtn.disabled = false;
    }
});

// Apagar uma rede
async function deleteNetwork(id) {
    if(confirm('Tem certeza que deseja apagar esta rede? As ESP32 não a encontrarão mais.')) {
        try {
            await fetch(`${API_URL}/${id}`, { method: 'DELETE' });
            await loadNetworks();
        } catch (error) {
            console.error('Erro ao apagar:', error);
        }
    }
}

// Inicia o carregamento quando a página abre
document.addEventListener('DOMContentLoaded', loadNetworks);