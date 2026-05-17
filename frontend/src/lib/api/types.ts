// Candidate types — matches backend CandidateArticleResponse
export interface CandidateArticle {
  id: number;
  title: string;
  chineseTitle: string | null;
  url: string;
  source: string;
  publishedAt: string;
  summary: string;
  chineseSummary: string | null;
  score: number | null;
  recommendationReason: string;
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
  paragraphIndex: number;
  englishText: string;
  chineseText: string | null;
}

export type VocabularyType = "WORD" | "PHRASE" | "EXPRESSION";

export interface VocabularyItem {
  id: number;
  word: string;
  lemma: string | null;
  type: VocabularyType;
  ipa: string | null;
  englishDefinition: string | null;
  chineseDefinition: string | null;
  sourceSentence: string | null;
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
  articleContent: string | null;
  summary: string;
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
  candidateArticleId: number;
  learningArticleId: number;
  status: LearningArticleStatus;
}

export type CreateLearningArticleResponse = SelectCandidateResponse;

export interface ChatMessage {
  id: number;
  role: string;
  content: string;
  createdAt: string;
}

export interface TutorChatResponse {
  sessionId: number;
  learningArticleId: number;
  reply: string;
  messages: ChatMessage[];
}

export interface TutorChatRequest {
  message: string;
  paragraphIndex?: number;
  selectedText?: string;
  mode?: "ASK" | "QUIZ" | "REVIEW";
  intent?: string;
}

export interface ChatSessionSummary {
  id: number;
  learningArticleId: number;
  title: string | null;
  createdAt: string;
  messageCount: number;
  lastMessageAt: string | null;
}
