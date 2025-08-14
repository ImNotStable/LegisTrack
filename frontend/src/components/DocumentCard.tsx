import React from 'react';
import { Link } from 'react-router-dom';
import { DocumentSummary } from '../types';
import {
  CalendarIcon,
  TagIcon,
  UserGroupIcon,
  ChartBarIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon
} from '@heroicons/react/24/outline';

interface DocumentCardProps {
  document: DocumentSummary;
}

export const DocumentCard: React.FC<DocumentCardProps> = ({ document }) => {
  const formatDate = (dateString?: string) => {
    if (!dateString) return 'Unknown';
    // Normalize to UTC to avoid timezone shifts in tests and across environments
    const normalized = `${dateString}T00:00:00Z`;
    return new Date(normalized).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      timeZone: 'UTC'
    });
  };

  const getStatusColor = (status?: string) => {
    switch (status?.toLowerCase()) {
      case 'passed':
        return 'bg-green-100 text-green-800';
      case 'failed':
        return 'bg-red-100 text-red-800';
      case 'pending':
        return 'bg-yellow-100 text-yellow-800';
      case 'introduced':
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const totalSponsors = document.partyBreakdown.total;
  const democraticPercentage = document.partyBreakdown.democraticPercentage;
  const republicanPercentage = document.partyBreakdown.republicanPercentage;

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow duration-200">
      <div className="p-6">
        {/* Header */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <Link
              to={`/document/${document.id}`}
              className="text-lg font-semibold text-gray-900 hover:text-blue-600 transition-colors duration-200"
            >
              {document.title}
            </Link>
            <div className="flex items-center mt-2 space-x-4 text-sm text-gray-500">
              <div className="flex items-center">
                <CalendarIcon className="h-4 w-4 mr-1" />
                {formatDate(document.introductionDate)}
              </div>
              {document.status && (
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(document.status)}`}>
                  {document.status}
                </span>
              )}
            </div>
          </div>

          {/* Analysis Status Indicator */}
          <div className="ml-4">
            {document.hasValidAnalysis ? (
              <div className="flex items-center text-green-600">
                <CheckCircleIcon className="h-5 w-5 mr-1" />
                <span className="text-xs font-medium">AI Analyzed</span>
              </div>
            ) : (
              <div className="flex items-center text-yellow-600">
                <ExclamationTriangleIcon className="h-5 w-5 mr-1" />
                <span className="text-xs font-medium">Pending Analysis</span>
              </div>
            )}
          </div>
        </div>

        {/* Party Breakdown */}
        {totalSponsors > 0 && (
          <div className="mb-4">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center text-sm text-gray-600">
                <UserGroupIcon className="h-4 w-4 mr-1" />
                <span>Sponsors ({totalSponsors})</span>
              </div>
              <div className="flex items-center text-xs text-gray-500">
                <ChartBarIcon className="h-3 w-3 mr-1" />
                Party Breakdown
              </div>
            </div>

            <div className="w-full bg-gray-200 rounded-full h-2 mb-2">
              <div className="flex h-2 rounded-full overflow-hidden">
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
                  style={{ width: `${100 - democraticPercentage - republicanPercentage}%` }}
                />
              </div>
            </div>

            <div className="flex justify-between text-xs text-gray-600">
              <span>D: {document.partyBreakdown.democratic} ({democraticPercentage.toFixed(1)}%)</span>
              <span>R: {document.partyBreakdown.republican} ({republicanPercentage.toFixed(1)}%)</span>
              {document.partyBreakdown.independent > 0 && (
                <span>I: {document.partyBreakdown.independent}</span>
              )}
            </div>
          </div>
        )}

        {/* Industry Tags */}
        {document.industryTags.length > 0 && (
          <div className="mb-4">
            <div className="flex items-center mb-2">
              <TagIcon className="h-4 w-4 mr-1 text-gray-400" />
              <span className="text-sm text-gray-600">Industry Tags</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {document.industryTags.slice(0, 3).map((tag, index) => (
                <Link
                  key={index}
                  to={`/?tag=${encodeURIComponent(tag)}`}
                  className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 hover:bg-blue-200 transition-colors duration-200"
                >
                  {tag}
                </Link>
              ))}
              {document.industryTags.length > 3 && (
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
                  +{document.industryTags.length - 3} more
                </span>
              )}
            </div>
          </div>
        )}

        {/* Action Button */}
        <div className="pt-4 border-t border-gray-100">
          <Link
            to={`/document/${document.id}`}
            className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-200"
          >
            View Details
          </Link>
        </div>
      </div>
    </div>
  );
};
