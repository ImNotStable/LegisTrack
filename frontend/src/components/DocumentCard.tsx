import React from 'react';
import { DocumentSummary } from '../types';
import { formatShortDate, getIndustryTagColor, truncateText } from '../utils';

interface DocumentCardProps {
  document: DocumentSummary;
}

export const DocumentCard: React.FC<DocumentCardProps> = ({ document }) => {
  // Calculate gradient based on party breakdown
  const getPartyGradient = () => {
    const { democraticPercentage, republicanPercentage } = document.partyBreakdown;
    
    if (democraticPercentage === 0 && republicanPercentage === 0) {
      return 'bg-gradient-to-r from-gray-50 to-gray-50';
    }
    
    // Create a more subtle gradient
    const blueWeight = democraticPercentage / 100;
    const redWeight = republicanPercentage / 100;
    
    if (blueWeight > redWeight) {
      return `bg-gradient-to-r from-blue-50 via-blue-25 to-red-25`;
    } else if (redWeight > blueWeight) {
      return `bg-gradient-to-r from-red-50 via-red-25 to-blue-25`;
    } else {
      return 'bg-gradient-to-r from-purple-50 to-purple-50';
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
        
        <div className="flex justify-between items-center">
          <div className="text-sm text-gray-600">
            <span className="font-medium">Sponsors:</span>{' '}
            <span className="text-blue-600">{document.partyBreakdown.democratic}D</span>
            {' / '}
            <span className="text-red-600">{document.partyBreakdown.republican}R</span>
            {document.partyBreakdown.independent > 0 && (
              <>
                {' / '}
                <span className="text-green-600">{document.partyBreakdown.independent}I</span>
              </>
            )}
            {document.partyBreakdown.other > 0 && (
              <>
                {' / '}
                <span className="text-gray-600">{document.partyBreakdown.other} Other</span>
              </>
            )}
          </div>
          
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
