import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeftIcon, CalendarIcon, TagIcon, UserGroupIcon, ClockIcon } from '@heroicons/react/24/outline';
import { CheckCircleIcon, ExclamationTriangleIcon } from '@heroicons/react/24/solid';
import { useDocumentById } from '../hooks/useApi';
import { LoadingSpinner } from './LoadingSpinner';
import { formatShortDate, getIndustryTagColor, formatLongDate } from '../utils';

// Tailwind safelist comment: bg-gradient-to-br from-red-400 via-red-300 to-red-200 from-blue-400 via-blue-300 to-blue-200 from-purple-400 via-purple-300 to-purple-200 from-red-100 to-white from-blue-100 to-white from-purple-100 to-white

export const DocumentDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const documentId = id ? parseInt(id) : 0;
  
  const { data: document, isLoading, error } = useDocumentById(documentId);

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
          <ExclamationTriangleIcon className="h-5 w-5 text-red-400" aria-hidden="true" />
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

  const getPartyGradient = () => {
    const { democraticPercentage, republicanPercentage, total } = document.partyBreakdown;
    
    // Only apply color if more than 10 people support the document
    if (total <= 10) {
      return 'bg-gray-50';
    }
    
    // Apply color based on political support thresholds
    if (republicanPercentage > 66) {
      // Strong Republican support: subtle red gradient
      return 'bg-gradient-to-br from-red-100 to-white';
    } else if (democraticPercentage > 66) {
      // Strong Democratic support: subtle blue gradient
      return 'bg-gradient-to-br from-blue-100 to-white';
    } else {
      // Mixed or moderate support: subtle purple gradient
      return 'bg-gradient-to-br from-purple-100 to-white';
    }
  };

  return (
    <div className="max-w-6xl mx-auto">
      {/* Header with back button */}
      <div className="mb-6">
        <button
          onClick={() => navigate('/')}
          className="inline-flex items-center text-blue-600 hover:text-blue-800 transition-colors duration-200"
        >
          <ArrowLeftIcon className="h-5 w-5 mr-2" />
          Back to Documents
        </button>
      </div>

      {/* Main document card */}
      <div className={`bg-white rounded-lg shadow-lg border border-gray-200 overflow-hidden ${getPartyGradient()}`}>
        {/* Header section */}
        <div className="p-6 border-b border-gray-200 bg-white/90 backdrop-blur-sm">
          <div className="flex justify-between items-start mb-4">
            <h1 className="text-2xl font-bold text-gray-900 flex-1 mr-4">
              {document.title}
            </h1>
            <span className="text-lg font-semibold text-gray-700 bg-gray-100 px-3 py-1 rounded-full">
              {document.billId}
            </span>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm text-gray-600">
            <div className="flex items-center">
              <CalendarIcon className="h-4 w-4 mr-2" />
              Introduced: {formatShortDate(document.introductionDate)}
            </div>
            <div className="flex items-center">
              <ClockIcon className="h-4 w-4 mr-2" />
              Congress: {document.congressSession}
            </div>
            <div className="flex items-center">
              <TagIcon className="h-4 w-4 mr-2" />
              Type: {document.billType}
            </div>
          </div>

          {document.status && (
            <div className="mt-3 text-sm text-gray-700 italic">
              Status: {document.status}
            </div>
          )}
        </div>

        {/* Official Summary */}
        {document.officialSummary && (
          <div className="p-6 border-b border-gray-200 bg-white/80 backdrop-blur-sm">
            <h2 className="text-lg font-semibold text-gray-900 mb-3">Official Summary</h2>
            <p className="text-gray-700 leading-relaxed">{document.officialSummary}</p>
          </div>
        )}

        {/* Industry Tags */}
        <div className="p-6 border-b border-gray-200 bg-white/80 backdrop-blur-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Industry Tags</h2>
          {document.analysis?.industryTags && document.analysis.industryTags.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {document.analysis.industryTags.map((tag, index) => (
                <span
                  key={index}
                  className={`tag ${getIndustryTagColor(tag)}`}
                >
                  {tag}
                </span>
              ))}
            </div>
          ) : (
            <p className="text-gray-500 italic">No industry tags available</p>
          )}
        </div>

        {/* AI Analysis */}
        <div className="p-6 border-b border-gray-200 bg-white/80 backdrop-blur-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">AI Analysis</h2>
            {document.analysis ? (
              <div className="flex items-center">
                {document.analysis.isValid ? (
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                    <CheckCircleIcon className="h-4 w-4 mr-1" />
                    Valid Analysis
                  </span>
                ) : (
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                    <ExclamationTriangleIcon className="h-4 w-4 mr-1" />
                    Under Review
                  </span>
                )}
              </div>
            ) : (
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                Pending Analysis
              </span>
            )}
          </div>

          {document.analysis ? (
            <div className="space-y-4">
              {document.analysis.generalEffectText && (
                <div>
                  <h3 className="font-medium text-gray-900 mb-2">General Effects</h3>
                  <p className="text-gray-700 leading-relaxed">{document.analysis.generalEffectText}</p>
                </div>
              )}
              
              {document.analysis.economicEffectText && (
                <div>
                  <h3 className="font-medium text-gray-900 mb-2">Economic Impact</h3>
                  <p className="text-gray-700 leading-relaxed">{document.analysis.economicEffectText}</p>
                </div>
              )}

              <div className="text-xs text-gray-500 pt-2 border-t border-gray-200">
                Analysis generated on {formatLongDate(document.analysis.analysisDate)}
                {document.analysis.modelUsed && ` using ${document.analysis.modelUsed}`}
              </div>
            </div>
          ) : (
            <p className="text-gray-500 italic">AI analysis is being processed and will be available shortly.</p>
          )}
        </div>

        {/* Sponsors */}
        <div className="p-6 border-b border-gray-200 bg-white/80 backdrop-blur-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Sponsors</h2>
          {document.sponsors && document.sponsors.length > 0 ? (
            <div className="space-y-3">
              {document.sponsors
                .sort((a, b) => (b.isPrimarySponsor ? 1 : 0) - (a.isPrimarySponsor ? 1 : 0))
                .map((sponsor, index) => (
                  <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div className="flex items-center">
                      <UserGroupIcon className="h-5 w-5 text-gray-400 mr-3" />
                      <div>
                        <div className="font-medium text-gray-900">
                          {sponsor.firstName} {sponsor.lastName}
                          {sponsor.isPrimarySponsor && (
                            <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                              Primary Sponsor
                            </span>
                          )}
                        </div>
                        <div className="text-sm text-gray-500">
                          {sponsor.party} • {sponsor.state}
                          {sponsor.district && ` (District ${sponsor.district})`}
                        </div>
                      </div>
                    </div>
                    {sponsor.sponsorDate && (
                      <div className="text-sm text-gray-500">
                        {formatShortDate(sponsor.sponsorDate)}
                      </div>
                    )}
                  </div>
                ))}
            </div>
          ) : (
            <p className="text-gray-500 italic">No sponsor information available</p>
          )}
        </div>

        {/* Recent Actions */}
        <div className="p-6 bg-white/80 backdrop-blur-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Recent Legislative Actions</h2>
          {document.actions && document.actions.length > 0 ? (
            <div className="space-y-3 max-h-64 overflow-y-auto">
              {document.actions
                .sort((a, b) => new Date(b.actionDate).getTime() - new Date(a.actionDate).getTime())
                .slice(0, 10)
                .map((action, index) => (
                  <div key={index} className="flex items-start p-3 bg-gray-50 rounded-lg">
                    <div className="flex-1">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm font-medium text-gray-900">
                          {formatShortDate(action.actionDate)}
                        </span>
                        {action.chamber && (
                          <span className="text-xs text-gray-500 bg-gray-200 px-2 py-1 rounded">
                            {action.chamber}
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-700">{action.actionText}</p>
                      {action.actionType && (
                        <span className="text-xs text-gray-500 italic">
                          {action.actionType}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
            </div>
          ) : (
            <p className="text-gray-500 italic">No legislative actions available</p>
          )}
        </div>

        {/* Footer with metadata */}
        <div className="px-6 py-4 bg-gray-50 border-t border-gray-200">
          <div className="flex justify-between items-center text-sm text-gray-500">
            <div>
              {document.fullTextUrl && (
                <a 
                  href={document.fullTextUrl} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:text-blue-800 underline"
                >
                  View Full Text →
                </a>
              )}
            </div>
            <div className="text-right">
              <div>Created: {formatLongDate(document.createdAt)}</div>
              <div>Updated: {formatLongDate(document.updatedAt)}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
