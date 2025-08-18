import {
	CalendarIcon,
	TagIcon,
	UserGroupIcon,
	ChartBarIcon,
	CheckCircleIcon,
	ExclamationTriangleIcon,
} from '@heroicons/react/24/outline';
import React, { memo, useMemo } from 'react';
import { Link } from 'react-router-dom';

import { DocumentSummary } from '../types';
import { formatShortDate, getStatusColor } from '../utils';

interface DocumentCardProps {
	document: DocumentSummary;
}

// Memoized component to prevent unnecessary re-renders
export const DocumentCard: React.FC<DocumentCardProps> = memo(({ document }) => {
	// Memoize computed values to avoid recalculation on each render
	const formattedDate = useMemo(
		() => formatShortDate(document.introductionDate),
		[document.introductionDate]
	);
	const statusColor = useMemo(() => getStatusColor(document.status), [document.status]);

	const {
		total: totalSponsors,
		democraticPercentage,
		republicanPercentage,
		democratic,
		republican,
		independent,
	} = document.partyBreakdown;

	// Memoize industry tags to prevent array recreation
	const displayTags = useMemo(() => document.industryTags.slice(0, 3), [document.industryTags]);
	const hasMoreTags = document.industryTags.length > 3;
	const additionalTagsCount = document.industryTags.length - 3;

	return (
		<article className="bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow duration-200">
			<div className="p-6">
				{/* Header */}
				<header className="flex items-start justify-between mb-4">
					<div className="flex-1">
						<Link
							to={`/document/${document.id}`}
							className="text-lg font-semibold text-gray-900 hover:text-blue-600 transition-colors duration-200"
							aria-label={`View details for ${document.title}`}
						>
							{document.title}
						</Link>
						<div className="flex items-center mt-2 space-x-4 text-sm text-gray-500">
							<div className="flex items-center">
								<CalendarIcon className="h-4 w-4 mr-1" aria-hidden="true" />
								<time dateTime={document.introductionDate}>{formattedDate}</time>
							</div>
							{document.status && (
								<span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColor}`}>
									{document.status}
								</span>
							)}
						</div>
					</div>

					{/* Analysis Status Indicator */}
					<div className="ml-4">
						{document.hasValidAnalysis ? (
							<div className="flex items-center text-green-600" title="AI analysis completed">
								<CheckCircleIcon className="h-5 w-5 mr-1" aria-hidden="true" />
								<span className="text-xs font-medium">AI Analyzed</span>
							</div>
						) : (
							<div className="flex items-center text-yellow-600" title="AI analysis pending">
								<ExclamationTriangleIcon className="h-5 w-5 mr-1" aria-hidden="true" />
								<span className="text-xs font-medium">Pending Analysis</span>
							</div>
						)}
					</div>
				</header>

				{/* Party Breakdown */}
				{totalSponsors > 0 && (
					<section className="mb-4" aria-labelledby="sponsors-heading">
						<div className="flex items-center justify-between mb-2">
							<h3 id="sponsors-heading" className="flex items-center text-sm text-gray-600">
								<UserGroupIcon className="h-4 w-4 mr-1" aria-hidden="true" />
								<span>Sponsors ({totalSponsors})</span>
							</h3>
							<div className="flex items-center text-xs text-gray-500">
								<ChartBarIcon className="h-3 w-3 mr-1" aria-hidden="true" />
								Party Breakdown
							</div>
						</div>

						<div
							className="w-full bg-gray-200 rounded-full h-2 mb-2"
							role="progressbar"
							aria-label="Party breakdown"
						>
							<div className="flex h-2 rounded-full overflow-hidden">
								<div
									className="bg-blue-500"
									style={{ width: `${democraticPercentage}%` }}
									title={`Democratic: ${democraticPercentage.toFixed(1)}%`}
								/>
								<div
									className="bg-red-500"
									style={{ width: `${republicanPercentage}%` }}
									title={`Republican: ${republicanPercentage.toFixed(1)}%`}
								/>
								<div
									className="bg-green-500"
									style={{
										width: `${Math.max(0, 100 - democraticPercentage - republicanPercentage)}%`,
									}}
									title={`Other: ${Math.max(0, 100 - democraticPercentage - republicanPercentage).toFixed(1)}%`}
								/>
							</div>
						</div>

						<div className="flex justify-between text-xs text-gray-600">
							<span>
								D: {democratic} ({democraticPercentage.toFixed(1)}%)
							</span>
							<span>
								R: {republican} ({republicanPercentage.toFixed(1)}%)
							</span>
							{independent > 0 && <span>I: {independent}</span>}
						</div>
					</section>
				)}

				{/* Industry Tags */}
				{document.industryTags.length > 0 && (
					<section className="mb-4" aria-labelledby="tags-heading">
						<div className="flex items-center mb-2">
							<TagIcon className="h-4 w-4 mr-1 text-gray-400" aria-hidden="true" />
							<h3 id="tags-heading" className="text-sm text-gray-600">
								Industry Tags
							</h3>
						</div>
						<div className="flex flex-wrap gap-2">
							{displayTags.map((tag, index) => (
								<Link
									key={index}
									to={`/?tag=${encodeURIComponent(tag)}`}
									className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 hover:bg-blue-200 transition-colors duration-200"
									aria-label={`Filter by ${tag} tag`}
								>
									{tag}
								</Link>
							))}
							{hasMoreTags && (
								<span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
									+{additionalTagsCount} more
								</span>
							)}
						</div>
					</section>
				)}

				{/* Action Button */}
				<footer className="pt-4 border-t border-gray-100">
					<Link
						to={`/document/${document.id}`}
						className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-200"
					>
						View Details
					</Link>
				</footer>
			</div>
		</article>
	);
});

DocumentCard.displayName = 'DocumentCard';
