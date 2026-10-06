const API_BASE = 'http://localhost:8080/api';

export async function loginUser(email, password) {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  const data = await res.json();

  if (!res.ok) {
    // Backend returns { error: "..." } or field-level errors like { email: "...", password: "..." }
    const message = data.error || Object.values(data).join(', ') || 'Login failed';
    throw new Error(message);
  }

  return data; // { token, tokenType, userId, fullName, email }
}

export async function registerUser(fullName, email, password) {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, email, password }),
  });

  const data = await res.json();

  if (!res.ok) {
    const message = data.error || Object.values(data).join(', ') || 'Registration failed';
    throw new Error(message);
  }

  return data; // { token, tokenType, userId, fullName, email }
}
