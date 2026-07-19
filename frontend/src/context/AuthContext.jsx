import { createContext, useContext, useEffect, useMemo, useState } from "react";
import * as authApi from "../api/authApi";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("accessToken"));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(token));

  const saveToken = (accessToken) => {
    localStorage.setItem("accessToken", accessToken);
    setToken(accessToken);
  };

  const clearSession = () => {
    localStorage.removeItem("accessToken");
    setToken(null);
    setUser(null);
  };

  const login = async (payload) => {
    const response = await authApi.login(payload);
    saveToken(response.data.accessToken);
    setUser(response.data.user);
    return response.data.user;
  };

  const register = async (payload) => {
    const response = await authApi.register(payload);
    saveToken(response.data.accessToken);
    setUser(response.data.user);
    return response.data.user;
  };

  const googleLogin = async (payload) => {
    const response = await authApi.googleLogin(payload);
    saveToken(response.data.accessToken);
    setUser(response.data.user);
    return response.data.user;
  };

  const refreshUser = async () => {
    if (!localStorage.getItem("accessToken")) {
      clearSession();
      return null;
    }

    try {
      const response = await authApi.getMe();
      setUser(response.data);
      return response.data;
    } catch (error) {
      clearSession();
      throw error;
    }
  };

  useEffect(() => {
    let mounted = true;

    const loadSession = async () => {
      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const response = await authApi.getMe();
        if (mounted) {
          setUser(response.data);
        }
      } catch {
        if (mounted) {
          clearSession();
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadSession();

    return () => {
      mounted = false;
    };
  }, [token]);

  const value = useMemo(
    () => ({
      token,
      user,
      loading,
      login,
      register,
      googleLogin,
      refreshUser,
      logout: clearSession
    }),
    [token, user, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
