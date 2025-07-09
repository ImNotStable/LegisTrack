import React from 'react';
import { DocumentSummary } from '../types';
import { formatShortDate, getIndustryTagColor, truncateText } from '../utils';

interface DocumentCardProps {
  document: DocumentSummary;
}

export const DocumentCard: React.FC<DocumentCardProps> = ({ document }) => {
  // Calculate gradient based on party breakdown (top-left to bottom-right)
  const getPartyGradient = () => {
    const { democraticPercentage, republicanPercentage, total } = document.partyBreakdown;
    
    // Default colorless state when no sponsors
    if (total === 0 || (democraticPercentage === 0 && republicanPercentage === 0)) {
      return '';
    }
    
    // Calculate gradient intensity based on political support
    const demIntensity = Math.min(Math.max(democraticPercentage, 10), 90);
    const repIntensity = Math.min(Math.max(republicanPercentage, 10), 90);
    
    // Create blue to purple to red gradient from top-left to bottom-right
    if (democraticPercentage > republicanPercentage) {
      // More Democratic support: blue → purple → light red
      return `bg-gradient-to-br from-blue-${Math.round(demIntensity / 20) * 100} via-purple-300 to-red-200`;
    } else if (republicanPercentage > democraticPercentage) {
      // More Republican support: light blue → purple → red
      return `bg-gradient-to-br from-blue-200 via-purple-300 to-red-${Math.round(repIntensity / 20) * 100}`;
    } else {
      // Equal support: balanced purple gradient
      return 'bg-gradient-to-br from-purple-300 via-purple-400 to-purple-300';
    }
  };

  return (
    <div className={`card p-6 mb-4 ${getPartyGradient()} border border-gray-200 transition-all duration-200`}>
        <div className="flex justify-between items-start mb-3">
          <h3 className="text-lg font-semibold text-gray-900 flex-1 mr-4">
            {truncateText(document.title, 120)}
          </h3>
          <span className="text-sm text-gray-500 whitespace-nowrap">
            {document.billId}
          </span>
        </div>
        
        <div className="flex justify-between items-center mb-3">
          <span className="text-sm text-gray-600">
            Introduced: {formatShortDate(document.introductionDate)}
          </span>
          {document.status && (
            <span className="text-sm text-gray-600 italic">
              {truncateText(document.status, 50)}
            </span>
          )}
        </div>
        
        {document.industryTags.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-3">
            {document.industryTags.slice(0, 5).map((tag, index) => (
              <span
                key={index}
                className={`tag ${getIndustryTagColor(tag)}`}
              >
                {tag}
              </span>
            ))}
            {document.industryTags.length > 5 && (
              <span className="tag bg-gray-100 text-gray-600">
                +{document.industryTags.length - 5} more
              </span>
            )}
          </div>
        )}
        
        <div className="flex justify-end items-center">
          <div className="flex items-center space-x-2">
            {document.hasValidAnalysis ? (
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                AI Analyzed
              </span>
            ) : (
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                Pending Analysis
              </span>
            )}
          </div>
        </div>
      </div>
  );
};
