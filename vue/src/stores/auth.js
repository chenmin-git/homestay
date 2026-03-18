import { defineStore } from 'pinia'
import http from '../api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('homestay-token') || '',
    user: JSON.parse(localStorage.getItem('homestay-user') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    restore() {
      this.token = localStorage.getItem('homestay-token') || ''
      this.user = JSON.parse(localStorage.getItem('homestay-user') || 'null')
    },
    async login(payload) {
      const result = await http.post('/auth/login', payload)
      this.applyAuth(result.data)
    },
    async register(payload) {
      const result = await http.post('/auth/register', payload)
      this.applyAuth(result.data)
    },
    applyAuth(data) {
      this.token = data.token
      this.user = data.user
      localStorage.setItem('homestay-token', data.token)
      localStorage.setItem('homestay-user', JSON.stringify(data.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('homestay-token')
      localStorage.removeItem('homestay-user')
    }
  }
})
