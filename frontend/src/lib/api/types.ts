// Candidate types — matches backend CandidateArticleResponse
export interface CandidateArticle {
  id: number;
  title: string;
  url: string;
  source: string;
  publishedAt: string;
  summary: string;
  llmScore: number;
  llmReason: string;
  selected: boolean;
}

// Learning article types — matches backend LearningArticleResponse
export type LearningArticleStatus =
  | "SELECTED"
  | "CONTENT_READY"
  | "TRANSLATED"
  | "VOCAB_READY"
  | "EUDIC_PUSHED"
  | "FAILED";

export interface ArticleParagraph {
  id: number;
  paragraphIndex: number;
  englishText: string;
  chineseText: string;
}

export type VocabularyType = "WORD" | "PHRASE" | "EXPRESSION";

export interface VocabularyItem {
  id: number;
  word: string;
  lemma: string;
  type: VocabularyType;
  ipa: string;
  englishDefinition: string;
  chineseDefinition: string;
  sourceSentence: string;
  eudicPushed: boolean;
}

export interface LearningArticle {
  id: number;
  candidateArticleId: number;
  status: LearningArticleStatus;
  title: string;
  url: string;
  source: string;
  publishedAt: string;
  summary: string;
  selectedAt: string;
  paragraphs: ArticleParagraph[];
  vocabularyItems: VocabularyItem[];
}

// Learning history types
export interface LearningHistoryItem {
  id: number;
  title: string;
  source: string;
  publishedAt: string;
  selectedAt: string;
  status: LearningArticleStatus;
}

// Selection response
export interface SelectCandidateResponse {
  learningArticleId: number;
}
