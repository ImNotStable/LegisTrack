import { PartyBreakdown } from '../types';

export const formatDate = (dateString?: string): string => {
  if (!dateString) return 'Unknown';
  
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return 'Invalid Date';
  }
};

export const formatShortDate = (dateString?: string): string => {
  if (!dateString) return 'Unknown';
  
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return 'Invalid Date';
  }
};

export const formatLongDate = (dateString?: string): string => {
  if (!dateString) return 'Unknown';
  
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return 'Invalid Date';
  }
};

export const getPartyColor = (party?: string): string => {
  if (!party) return 'gray';
  
  const p = party.toUpperCase();
  if (p.startsWith('D') || p === 'DEM' || p === 'DEMOCRATIC') return 'blue';
  if (p.startsWith('R') || p === 'REP' || p === 'REPUBLICAN') return 'red';
  if (p.startsWith('I') || p === 'IND' || p === 'INDEPENDENT') return 'green';
  return 'gray';
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

export const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '...';
};

export const getIndustryTagColor = (tag: string): string => {
  const colors = [
    'bg-blue-100 text-blue-800',
    'bg-green-100 text-green-800',
    'bg-yellow-100 text-yellow-800',
    'bg-red-100 text-red-800',
    'bg-purple-100 text-purple-800',
    'bg-pink-100 text-pink-800',
    'bg-indigo-100 text-indigo-800',
  ];
  
  // Simple hash function to consistently assign colors
  let hash = 0;
  for (let i = 0; i < tag.length; i++) {
    hash = tag.charCodeAt(i) + ((hash << 5) - hash);
  }
  
  return colors[Math.abs(hash) % colors.length];
};

export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout;
  
  return (...args: Parameters<T>) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
};
