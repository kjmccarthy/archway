const BASE_URL = "https://private-a63a5-heimdali.apiary-mock.com";

export function login(username, password) {
    return fetch(BASE_URL + "/account/token", {
        headers: {
            "Authorization": "Basic " + btoa(username + ":" + password),
        }
    }).then(response => response.json());
}

export function logout() {

}

export function workspace(token, prefer = 404) {
    return fetch(BASE_URL + "/account/workspace", {
        headers: {
            "Authorization": "Bearer " + token,
            "Prefer": "status=" + prefer
        }
    }).then(response => response.json());
}

export function requestWorkspace(token) {
    return fetch(BASE_URL + "/account/workspace", {
        method: 'POST',
        headers: {
            "Authorization": "Bearer " + token
        }
    });
}

export function cluster() {
    return fetch(BASE_URL + "/clusters", {})
        .then(response => response.json());
}

export function profile(token) {
    return fetch(BASE_URL + "/account/profile", {
        headers: {
            "Authorization": "Bearer " + token
        }
    }).then(response => response.json());
}

export function sharedWorkspaces(token) {
    return fetch(BASE_URL + "/workspaces", {
        headers: {
            "Authorization": "Bearer " + token
        }
    }).then(response => response.json());
}

export function sharedWorkspaceDetails(token, id) {
    return fetch(BASE_URL + `/workspaces/${id}`, {
        headers: {
            "Authorization": "Bearer " + token
        }
    }).then(response => response.json());
}

export function requestNewSharedWorkspace(token, workspace) {

    return fetch(BASE_URL + "/workspaces", {
        method: "POST",
        body: JSON.stringify(workspace),
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
            "Authorization": "Bearer " + token
        }
    }).then(response => response.json());
}

export function workspaceMemberList(token, id) {
    return fetch(BASE_URL + "/workspaces/"+id + "/members", {
        headers: {
            "Authorization": "Bearer " + token
        }
    }).then(response => response.json());
}

export function informationAreas(token) {
    return fetch(BASE_URL + "/areas", {
        headers: {
            "Authorization": "Bearer " + token
        }
    }).then(response => response.json());
}