import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DocumentCard } from '../DocumentCard';
import { DocumentSummary } from '../types';

const mockDocument: DocumentSummary = {
  id: 1,
  billId: 'H.R.1234',
  title: 'Test Healthcare Reform Bill',
  introductionDate: '2024-01-15',
  status: 'introduced',
  industryTags: ['Healthcare', 'Insurance', 'Public Health'],
  partyBreakdown: {
    democratic: 25,
    republican: 10,
    independent: 2,
    other: 0,
    total: 37,
    democraticPercentage: 67.6,
    republicanPercentage: 27.0,
  },
  hasValidAnalysis: true,
};

const renderWithProviders = (component: React.ReactElement) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        {component}
      </BrowserRouter>
    </QueryClientProvider>
  );
};

describe('DocumentCard', () => {
  it('renders document information correctly', () => {
    renderWithProviders(<DocumentCard document={mockDocument} />);

    expect(screen.getByText('Test Healthcare Reform Bill')).toBeInTheDocument();
    expect(screen.getByText('Jan 15, 2024')).toBeInTheDocument();
    expect(screen.getByText('introduced')).toBeInTheDocument();
    expect(screen.getByText('Sponsors (37)')).toBeInTheDocument();
  });

  it('displays AI analysis status correctly', () => {
    renderWithProviders(<DocumentCard document={mockDocument} />);

    expect(screen.getByText('AI Analyzed')).toBeInTheDocument();
  });

  it('shows pending analysis for documents without analysis', () => {
    const documentWithoutAnalysis = { ...mockDocument, hasValidAnalysis: false };
    renderWithProviders(<DocumentCard document={documentWithoutAnalysis} />);

    expect(screen.getByText('Pending Analysis')).toBeInTheDocument();
  });

  it('displays party breakdown correctly', () => {
    renderWithProviders(<DocumentCard document={mockDocument} />);

    expect(screen.getByText('D: 25 (67.6%)')).toBeInTheDocument();
    expect(screen.getByText('R: 10 (27.0%)')).toBeInTheDocument();
  });

  it('renders industry tags with correct links', () => {
    renderWithProviders(<DocumentCard document={mockDocument} />);

    const healthcareTag = screen.getByText('Healthcare');
    expect(healthcareTag.closest('a')).toHaveAttribute('href', '/?tag=Healthcare');
  });

  it('shows limited number of tags with overflow indicator', () => {
    const documentWithManyTags = {
      ...mockDocument,
      industryTags: ['Healthcare', 'Insurance', 'Public Health', 'Medical', 'Reform'],
    };

    renderWithProviders(<DocumentCard document={documentWithManyTags} />);

    expect(screen.getByText('+2 more')).toBeInTheDocument();
  });

  it('navigates to document detail when clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DocumentCard document={mockDocument} />);

    const titleLink = screen.getByText('Test Healthcare Reform Bill');
    await user.click(titleLink);

    // Check that the href is correct
    expect(titleLink.closest('a')).toHaveAttribute('href', '/document/1');
  });

  it('renders view details button', () => {
    renderWithProviders(<DocumentCard document={mockDocument} />);

    const viewDetailsButton = screen.getByText('View Details');
    expect(viewDetailsButton).toBeInTheDocument();
    expect(viewDetailsButton.closest('a')).toHaveAttribute('href', '/document/1');
  });

  it('handles missing optional data gracefully', () => {
    const incompleteDocument: DocumentSummary = {
      id: 2,
      billId: 'S.456',
      title: 'Incomplete Bill',
      industryTags: [],
      partyBreakdown: {
        democratic: 0,
        republican: 0,
        independent: 0,
        other: 0,
        total: 0,
        democraticPercentage: 0,
        republicanPercentage: 0,
      },
      hasValidAnalysis: false,
    };

    renderWithProviders(<DocumentCard document={incompleteDocument} />);

    expect(screen.getByText('Incomplete Bill')).toBeInTheDocument();
    expect(screen.getByText('Unknown')).toBeInTheDocument(); // Date should show as Unknown
  });
});
