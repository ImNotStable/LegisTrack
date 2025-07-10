import React from 'react';
import { useNavigate } from 'react-router-dom';
import { DocumentSummary } from '../types';
import { formatShortDate, getIndustryTagColor, truncateText } from '../utils';

// Tailwind safelist comment: bg-gradient-to-br from-red-400 via-red-300 to-red-200 from-blue-400 via-blue-300 to-blue-200 from-purple-400 via-purple-300 to-purple-200 from-red-100 to-white from-blue-100 to-white from-purple-100 to-white

interface DocumentCardProps {
  document: DocumentSummary;
}

export const DocumentCard: React.FC<DocumentCardProps> = ({ document }) => {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(`/document/${document.id}`);
  };

  // Calculate gradient based on party breakdown (top-left to bottom-right)
  const getPartyGradient = () => {
    const { democraticPercentage, republicanPercentage, total } = document.partyBreakdown;
    
    // Only apply color if more than 10 people support the document
    if (total <= 10) {
      return '';
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
    <div 
      className={`card p-6 mb-4 ${getPartyGradient()} border border-gray-200 transition-all duration-200 cursor-pointer hover:shadow-lg transform hover:scale-[1.02]`}
      onClick={handleClick}
    >
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
