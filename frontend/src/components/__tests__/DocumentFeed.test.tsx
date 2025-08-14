import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { rest } from 'msw';
import { DocumentFeed } from '../DocumentFeed';
import { server } from '../../mocks/handlers';

// Setup MSW
beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

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

describe('DocumentFeed', () => {
  it('renders loading spinner initially', () => {
    renderWithProviders(<DocumentFeed />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('displays documents after loading', async () => {
    renderWithProviders(<DocumentFeed />);

    await waitFor(() => {
      expect(screen.getByText('Test Bill for Healthcare Reform')).toBeInTheDocument();
    });
  });

  it('shows document count and pagination info', async () => {
    renderWithProviders(<DocumentFeed />);

    await waitFor(() => {
      expect(screen.getByText(/Showing 1 of 1 documents/)).toBeInTheDocument();
    });
  });

  it('allows sorting documents', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DocumentFeed />);

    const sortSelect = await screen.findByRole('combobox');
    // Default value should be introductionDate_desc
    expect(sortSelect).toHaveValue('introductionDate_desc');

    await user.selectOptions(sortSelect, 'title_asc');
    expect(sortSelect).toHaveValue('title_asc');
  });

  it('handles pagination correctly', async () => {
    renderWithProviders(<DocumentFeed />);

    await waitFor(() => {
      // Should not show pagination for single page
      expect(screen.queryByText('Previous')).not.toBeInTheDocument();
      expect(screen.queryByText('Next')).not.toBeInTheDocument();
    });
  });

  it('displays error message when API fails', async () => {
    // Mock API failure
    server.use(
      rest.get('http://localhost:8080/api/documents', (req, res, ctx) => {
        return res(ctx.status(500), ctx.json({ message: 'Server error' }));
      })
    );

    renderWithProviders(<DocumentFeed />);

    await waitFor(() => {
      expect(screen.getByText('Error loading documents')).toBeInTheDocument();
    });
  });
});
