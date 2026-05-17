
// delete travmail
function deleteMail(id, emailAddress) {
    if (!confirm("Are you sure you want to delete " + emailAddress + "? You will no longer receive emails at this address after deletion")) {
        return;
    }
    fetch('/api/email/delete/' + id, { method: 'DELETE' })
        .then(response => {
            if (response.ok) {
                alert("Your TravMail (" + emailAddress + ") is successfully deleted.");
                window.location.reload();
            } else {
                response.text().then(text => alert("Deletion Failed: " + text)).catch(() => alert("Failed to delete the email."));
            }
        }).catch(error => console.error('Error:', error));
}

// invite companions
function sendInvitation(travMailId) {
    const emailInput = document.getElementById('companionEmail-' + travMailId);
    const email = emailInput.value;

    if (!email || !email.includes('@')) {
        alert('Please enter a valid email address.');
        return;
    }

    fetch('/api/email/companion/invite', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ 'travMailId': travMailId, 'email': email })
    })
        .then(response => {
            if (response.ok) {
                alert('Invitation sent successfully! Waiting for the companion to accept.');
                emailInput.value = '';
                window.location.reload();
            } else {
                response.text().then(text => alert("Failed to send invitation: " + text)).catch(() => alert("Failed to send invitation."));
            }
        }).catch(error => console.error('Error:', error));
}

// delete companions
function removeCompanion(travMailId, email) {
    if (!confirm("Remove " + email + " from the Companion List?")) return;

    fetch(`/api/email/companion/remove?travMailId=${travMailId}&email=${email}`, { method: 'DELETE' })
        .then(response => {
            if (response.ok) {
                alert('Companion removed successfully.');
                window.location.reload();
            } else {
                alert('Failed to remove companion.');
            }
        }).catch(error => console.error('Error:', error));
}

// pause
function togglePause(travMailId, currentStatus) {
    const newStatus = !currentStatus;
    // call api
    fetch(`/api/email/pause?travMailId=${travMailId}&pause=${newStatus}`, { method: 'POST' })
        .then(response => {
            if (response.ok) {
                window.location.reload();
            } else {
                alert('Failed to update status.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Server Error');
        });
}

// copy travmail
function copyToClipboard(text, button) {
    navigator.clipboard.writeText(text).then(() => {
        const icon = button.querySelector('i');
        icon.classList.replace('fa-copy', 'fa-check');
        button.classList.replace('text-secondary', 'text-success');

        setTimeout(() => {
            icon.classList.replace('fa-check', 'fa-copy');
            button.classList.replace('text-success', 'text-secondary');
        }, 2000);
    }).catch(err => alert('Failed to copy: ' + err));
}