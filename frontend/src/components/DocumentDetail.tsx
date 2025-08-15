import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeftIcon,
  CalendarIcon,
  UserGroupIcon,
  ClockIcon,
  DocumentTextIcon,
  ArrowTopRightOnSquareIcon,
  ArrowPathIcon,
  SparklesIcon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon, ExclamationTriangleIcon } from '@heroicons/react/24/solid';
import { useDocument, useRefreshDocument, useAnalyzeDocument } from '../hooks/useApi';
import { LoadingSpinner } from './LoadingSpinner';
import { useToast } from './Toast';
import { useState } from 'react';

export const DocumentDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: document, isLoading, error, refetch } = useDocument(id!);
  const refreshMutation = useRefreshDocument();
  const analyzeMutation = useAnalyzeDocument();
  const toast = useToast();
  const [, setIsPolling] = useState(false);

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const formatShortDate = (dateString?: string) => {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const handleRefresh = () => {
    if (id) {
      refreshMutation.mutate(id, {
        onSuccess: () => toast.success('Document refreshed.'),
        onError: () => toast.error('Failed to refresh document.'),
      });
    }
  };

  const handleAnalyze = () => {
    if (id) {
      const toastId = toast.info('Starting AI analysis...', { durationMs: 0 });
      analyzeMutation.mutate(id, {
        onSuccess: () => {
          toast.dismiss(toastId);
          const pollingToast = toast.info('Analyzing... this may take up to a minute.', { durationMs: 0 });
          setIsPolling(true);
          const maxAttempts = 20;
          let attempts = 0;
          const poll = async () => {
            attempts += 1;
            try {
              const result = await refetch();
              if (result.data && (result.data as any).analysis) {
                toast.dismiss(pollingToast);
                toast.success('AI analysis completed.');
                setIsPolling(false);
                return;
              }
            } catch (_e) {
              // ignore and keep polling
            }
            if (attempts < maxAttempts) {
              setTimeout(poll, 3000);
            } else {
              toast.dismiss(pollingToast);
              toast.warning('Analysis is taking longer than expected. It will appear when ready.');
              setIsPolling(false);
            }
          };
          setTimeout(poll, 2000);
        },
        onError: (e) => {
          toast.dismiss(toastId);
          toast.error('AI analysis failed.');
        },
      });
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  if (error || !document) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-md p-4">
        <div className="flex">
          <ExclamationTriangleIcon className="h-5 w-5 text-red-400" />
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">
              Error loading document
            </h3>
            <div className="mt-2 text-sm text-red-700">
              <p>Could not find the requested document. It may have been removed or the link is invalid.</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const totalSponsors = document.partyBreakdown.total;
  const democraticPercentage = document.partyBreakdown.democraticPercentage;
  const republicanPercentage = document.partyBreakdown.republicanPercentage;

  return (
    <div className="max-w-4xl mx-auto">
      {/* Back Navigation */}
      <div className="mb-6">
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center text-sm text-gray-500 hover:text-gray-700"
        >
          <ArrowLeftIcon className="h-4 w-4 mr-1" />
          Back to Documents
        </button>
      </div>

      {/* Document Header */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-6">
        <div className="flex justify-between items-start mb-4">
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-gray-900 mb-2">
              {document.title}
            </h1>
            <div className="flex items-center text-sm text-gray-500 space-x-4">
              <div className="flex items-center">
                <CalendarIcon className="h-4 w-4 mr-1" />
                Introduced: {formatDate(document.introductionDate)}
              </div>
              <div className="flex items-center">
                <DocumentTextIcon className="h-4 w-4 mr-1" />
                {document.billType} {document.billId}
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex space-x-2">
            <button
              onClick={handleRefresh}
              disabled={refreshMutation.isPending}
              className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              <ArrowPathIcon className={`h-4 w-4 mr-1 ${refreshMutation.isPending ? 'animate-spin' : ''}`} />
              Refresh
            </button>

            {!document.analysis?.isValid && (
              <button
                onClick={handleAnalyze}
                disabled={analyzeMutation.isPending}
                className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
              >
                <SparklesIcon className="h-4 w-4 mr-1" />
                {analyzeMutation.isPending ? 'Analyzing...' : 'Analyze with AI'}
              </button>
            )}
          </div>
        </div>

        {/* Official Summary */}
        {document.officialSummary && (
          <div className="mb-4 p-4 bg-gray-50 rounded-lg">
            <h3 className="text-sm font-medium text-gray-900 mb-2">Official Summary</h3>
            <p className="text-sm text-gray-700">{document.officialSummary}</p>
          </div>
        )}

        {/* External Links */}
        {document.fullTextUrl && (
          <div className="mb-4">
            <a
              href={document.fullTextUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center text-sm text-blue-600 hover:text-blue-500"
            >
              <ArrowTopRightOnSquareIcon className="h-4 w-4 mr-1" />
              {document.fullTextUrl.includes('congress.gov') ? 'View Full Text on Congress.gov' : 'View Full Text'}
            </a>
          </div>
        )}
      </div>

      {/* Sponsors Section */}
      {document.sponsors.length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900 flex items-center">
              <UserGroupIcon className="h-5 w-5 mr-2" />
              Sponsors ({totalSponsors})
            </h2>
          </div>

          {/* Party Breakdown */}
          <div className="mb-4">
            <div className="w-full bg-gray-200 rounded-full h-3 mb-3">
              <div className="flex h-3 rounded-full overflow-hidden">
                <div
                  className="bg-blue-500"
                  style={{ width: `${democraticPercentage}%` }}
                />
                <div
                  className="bg-red-500"
                  style={{ width: `${republicanPercentage}%` }}
                />
                <div
                  className="bg-green-500"
                  style={{ width: `${Math.max(0, 100 - democraticPercentage - republicanPercentage)}%` }}
                />
              </div>
            </div>

            <div className="flex justify-between text-sm text-gray-600">
              <span>Democrats: {document.partyBreakdown.democratic} ({democraticPercentage.toFixed(1)}%)</span>
              <span>Republicans: {document.partyBreakdown.republican} ({republicanPercentage.toFixed(1)}%)</span>
              {document.partyBreakdown.independent > 0 && (
                <span>Independent: {document.partyBreakdown.independent}</span>
              )}
            </div>
          </div>

          {/* Sponsors List */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {document.sponsors.map((sponsor) => (
              <div key={sponsor.id} className="p-3 bg-gray-50 rounded-lg">
                <div className="font-medium text-sm text-gray-900">
                  {sponsor.firstName} {sponsor.lastName}
                  {sponsor.isPrimarySponsor && (
                    <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                      Primary
                    </span>
                  )}
                </div>
                <div className="text-xs text-gray-500">
                  {sponsor.party} - {sponsor.state}
                  {sponsor.district && ` (District ${sponsor.district})`}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* AI Analysis Section */}
      {document.analysis && document.analysis.isValid && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex items-center mb-4">
            <CheckCircleIcon className="h-5 w-5 text-green-500 mr-2" />
            <h2 className="text-lg font-semibold text-gray-900">AI Analysis</h2>
            <span className="ml-2 text-xs text-gray-500">
              Analyzed on {formatShortDate(document.analysis.analysisDate)}
            </span>
          </div>

          {document.analysis.generalEffectText && (
            <div className="mb-4">
              <h3 className="text-sm font-medium text-gray-900 mb-2">General Impact</h3>
              <p className="text-sm text-gray-700">{document.analysis.generalEffectText}</p>
            </div>
          )}

          {document.analysis.economicEffectText && (
            <div className="mb-4">
              <h3 className="text-sm font-medium text-gray-900 mb-2">Economic Impact</h3>
              <p className="text-sm text-gray-700">{document.analysis.economicEffectText}</p>
            </div>
          )}

          {document.analysis.industryTags.length > 0 && (
            <div>
              <h3 className="text-sm font-medium text-gray-900 mb-2">Industry Tags</h3>
              <div className="flex flex-wrap gap-2">
                {document.analysis.industryTags.map((tag, index) => (
                  <span
                    key={index}
                    className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Actions Timeline */}
      {document.actions.length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <ClockIcon className="h-5 w-5 mr-2" />
            Legislative Actions
          </h2>
          <div className="space-y-4">
            {document.actions.slice(0, 10).map((action) => (
              <div key={action.id} className="flex">
                <div className="flex-shrink-0 w-24 text-xs text-gray-500">
                  {formatShortDate(action.actionDate)}
                </div>
                <div className="flex-1 ml-4">
                  <p className="text-sm text-gray-900">{action.actionText}</p>
                  {action.chamber && (
                    <p className="text-xs text-gray-500 mt-1">
                      Chamber: {action.chamber}
                    </p>
                  )}
                </div>
              </div>
            ))}
            {document.actions.length > 10 && (
              <p className="text-sm text-gray-500 italic">
                ... and {document.actions.length - 10} more actions
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
