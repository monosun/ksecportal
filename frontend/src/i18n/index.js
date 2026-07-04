import { createI18n } from 'vue-i18n'
import ko from './ko.json'
import en from './en.json'

const savedLocale = localStorage.getItem('locale') || 'ko'

export default createI18n({
  legacy: false,
  locale: savedLocale,
  fallbackLocale: 'en',
  messages: { ko, en }
})
