import { PartyBreakdown } from '../types';

// Date formatting utilities
export const formatDate = (dateString?: string, format: 'short' | 'long' | 'simple' = 'long') => {
  if (!dateString) return 'Unknown';
  
  const date = new Date(dateString);

  switch (format) {
    case 'short':
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    case 'simple':
      return date.toLocaleDateString('en-US');
    case 'long':
    default:
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
  }
};

export const formatShortDate = (dateString?: string) => formatDate(dateString, 'short');
export const formatLongDate = (dateString?: string) => formatDate(dateString, 'long');
export const formatSimpleDate = (dateString?: string) => formatDate(dateString, 'simple');

// Time utilities
export const timeAgo = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diffInSeconds < 60) return 'Just now';
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} minutes ago`;
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`;
  if (diffInSeconds < 2592000) return `${Math.floor(diffInSeconds / 86400)} days ago`;
  if (diffInSeconds < 31536000) return `${Math.floor(diffInSeconds / 2592000)} months ago`;
  return `${Math.floor(diffInSeconds / 31536000)} years ago`;
};

// Party breakdown utilities
export const getPartyColor = (party: string): string => {
  switch (party.toLowerCase()) {
    case 'democratic':
    case 'democrat':
    case 'd':
      return 'blue';
    case 'republican':
    case 'r':
      return 'red';
    case 'independent':
    case 'i':
      return 'green';
    default:
      return 'gray';
  }
};

export const getPartyAbbreviation = (party: string): string => {
  switch (party.toLowerCase()) {
    case 'democratic':
    case 'democrat':
      return 'D';
    case 'republican':
      return 'R';
    case 'independent':
      return 'I';
    default:
      return party.charAt(0).toUpperCase();
  }
};

export const getPartyGradient = (partyBreakdown: PartyBreakdown): string => {
  const { democraticPercentage, republicanPercentage } = partyBreakdown;
  
  if (democraticPercentage === 0 && republicanPercentage === 0) {
    return 'bg-gradient-to-r from-gray-100 to-gray-100';
  }
  
  // Calculate gradient stops based on percentages
  const blueIntensity = Math.max(10, democraticPercentage);
  const redIntensity = Math.max(10, republicanPercentage);
  
  return `bg-gradient-to-r from-blue-${Math.round(blueIntensity / 10) * 100} via-purple-200 to-red-${Math.round(redIntensity / 10) * 100}`;
};

// Industry tag utilities
export const getIndustryTagColor = (tag: string): string => {
  const colors = [
    'blue', 'green', 'yellow', 'red', 'purple', 'pink', 'indigo', 'gray'
  ];
  
  // Simple hash function to consistently assign colors
  let hash = 0;
  for (let i = 0; i < tag.length; i++) {
    hash = tag.charCodeAt(i) + ((hash << 5) - hash);
  }
  
  return colors[Math.abs(hash) % colors.length];
};

// Status utilities
export const getStatusColor = (status?: string): string => {
  switch (status?.toLowerCase()) {
    case 'passed':
      return 'bg-green-100 text-green-800';
    case 'failed':
      return 'bg-red-100 text-red-800';
    case 'pending':
      return 'bg-yellow-100 text-yellow-800';
    case 'introduced':
      return 'bg-blue-100 text-blue-800';
    case 'committee':
      return 'bg-purple-100 text-purple-800';
    case 'floor':
      return 'bg-orange-100 text-orange-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

// Text utilities
export const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength).trim() + '...';
};

export const highlightSearchTerms = (text: string, searchTerm: string): string => {
  if (!searchTerm) return text;

  const regex = new RegExp(`(${searchTerm})`, 'gi');
  return text.replace(regex, '<mark class="bg-yellow-200">$1</mark>');
};

// Number utilities
export const formatNumber = (num: number): string => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M';
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K';
  }
  return num.toString();
};

export const formatPercentage = (value: number, total: number): string => {
  if (total === 0) return '0%';
  return ((value / total) * 100).toFixed(1) + '%';
};

// Validation utilities
export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

export const isValidUrl = (url: string): boolean => {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
};

// Error handling utilities
export const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return 'An unexpected error occurred';
};

// Local storage utilities
export const storage = {
  get: <T>(key: string, defaultValue: T): T => {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : defaultValue;
    } catch {
      return defaultValue;
    }
  },

  set: <T>(key: string, value: T): void => {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      console.warn('Failed to save to localStorage:', error);
    }
  },

  remove: (key: string): void => {
    try {
      localStorage.removeItem(key);
    } catch (error) {
      console.warn('Failed to remove from localStorage:', error);
    }
  }
};

// Debounce utility
export const debounce = <T extends (...args: any[]) => void>(
  func: T,
  wait: number
): T => {
  let timeout: NodeJS.Timeout;
  
  return ((...args: any[]) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  }) as T;
};
