import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

function Profile() {
  const [name, setName] = useState(localStorage.getItem('userName') || '');
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [profileMsg, setProfileMsg] = useState('');
  const [passwordMsg, setPasswordMsg] = useState('');
  const [error, setError] = useState('');

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setError('');
    setProfileMsg('');
    try {
      const res = await api.put('/auth/profile', { name });
      localStorage.setItem('userName', res.data.name);
      localStorage.setItem('token', res.data.token);
      setProfileMsg('Name updated successfully.');
    } catch (err) {
      setError('Failed to update profile');
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setError('');
    setPasswordMsg('');
    try {
      await api.post('/auth/change-password', { oldPassword, newPassword });
      setOldPassword('');
      setNewPassword('');
      setPasswordMsg('Password changed successfully.');
    } catch (err) {
      setError('Failed to change password - check your current password.');
    }
  };

  return (
    <div>
      <div className="nav-bar">
        <h2>Profile Settings</h2>
        <Link to="/dashboard">Back to Dashboard</Link>
      </div>

      {error && <div className="message error">{error}</div>}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Update Name</h3>
        {profileMsg && <div className="message success">{profileMsg}</div>}
        <form onSubmit={handleUpdateProfile}>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Your name"
            required
          />
          <button type="submit">Save Name</button>
        </form>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Change Password</h3>
        {passwordMsg && <div className="message success">{passwordMsg}</div>}
        <form onSubmit={handleChangePassword}>
          <input
            type="password"
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
            placeholder="Current password"
            required
          />
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="New password (min 6 characters)"
            required
          />
          <button type="submit">Change Password</button>
        </form>
      </div>
    </div>
  );
}

export default Profile;