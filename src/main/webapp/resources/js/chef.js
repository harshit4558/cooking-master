function handleAjax(data) {
    if (data.status === 'begin') {
        document.getElementById('loadingOverlay').style.display = 'flex';
    } else if (data.status === 'complete') {
        document.getElementById('loadingOverlay').style.display = 'none';
    }
} 