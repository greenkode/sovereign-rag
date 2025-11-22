# Feature Extraction from Progress Agentic RAG Screenshots

## Overview
This document catalogs all features extracted from 30 screenshots of Progress Software's Agentic RAG product, organized chronologically and by functional area.

## Product Information
- **Product Name**: Progress Agentic RAG
- **Version**: 0.0.1-haiku2e40c512
- **Branding**: Progress Software with green arrow logo
- **Account Type**: Trial Account available

---

## 1. NAVIGATION & LAYOUT

### 1.1 Main Navigation (Left Sidebar)
The application uses a persistent left sidebar with the following menu items:

1. **Home** - Dashboard/landing page
2. **Upload data** - Content ingestion interface
3. **Resources list** - Browse uploaded resources
4. **Synchronize** - Connect external data sources
5. **Search** - Test search and configure search behavior
6. **Widgets** - Create and manage embeddable widgets
7. **Advanced** - Expandable section containing:
   - **Settings** - Knowledge Box configuration
   - **AI models** - LLM and embedding model selection
   - **Agents** - (Premium feature)
   - **Metrics** - RAG evaluation metrics (REMI)
   - **Activity log** - (Premium feature)
   - **API keys** - Manage API access
   - **Label sets** - Create taxonomy/classification labels
   - **NER** - Named Entity Recognition (Premium feature)
   - **RAG lab** - (Premium feature)
   - **Users** - User management and roles

### 1.2 Top Navigation Bar
- **Left**: Product logo and name "Progress Agentic RAG"
- **Center**: "Need help? Book a demo!" button (blue outline)
- **Right**:
  - "Trial Account" badge
  - Notification bell icon
  - User avatar circle with initials "UB"

---

## 2. DATA INGESTION FEATURES

### 2.1 Upload Data Page
**Purpose**: Direct file and resource upload to the Knowledge Box

**Upload Types Supported**:
1. **File** - Individual file upload (document icon)
2. **Folder** - Batch folder upload (folder icon)
3. **Links** - URL/web page ingestion (link icon)
4. **Text resources** - Manual text entry (document icon)
5. **Sitemap** - XML sitemap crawling (globe with lines icon)
6. **Q&A** - Question-answer pairs (chat bubbles icon)

**Additional Features**:
- Info message: "To synchronize data from third-party sources or local folders, head to the Synchronize section"
- "Go" button to navigate to Synchronize

### 2.2 Synchronize Page
**Purpose**: Connect to external data sources for automatic synchronization

**Sync Server Options**:
1. **Use the Agentic RAG Sync Agent on your desktop**
   - Status indicator: "INACTIVE"
   - Download link for local installation
   - Description: "To install the sync server locally, download the latest version for your system"

2. **Use a remote server**
   - Deploy on server with instructions link
   - For enterprise deployments

**Sync Sources Available**:
1. **Folder** - Local/network folders (yellow folder icon)
2. **RSS** - RSS feed synchronization (orange RSS icon)
3. **Sitemap** - XML sitemap auto-sync (gray sitemap icon)

**Synchronizations Management**:
- Section to manage configured syncs
- Message: "Your syncs will appear here"
- Info: "You can manage here your different sources, your sync agent must be active"

---

## 3. RESOURCES LIST

### 3.1 Empty State
**Display**: "Nothing to see here...yet"
**Message**: "Your resource list is empty. Start by uploading some files or synchronizing your data."
**Action Button**: "Upload data" (blue button)

---

## 4. SEARCH CONFIGURATION

### 4.1 Search Testing Interface
**Main Elements**:
- Search input box: "Type your question here"
- Description: "Try Agentic RAG search on your Knowledge Box and set up your search configuration"

### 4.2 Saved Configurations Panel
**Features**:
- Current saved configuration display
- Configuration name: "Standard answer generation - Agentic RAG"
- Agent info: "OpenAI + Azure ChatGPT-4o"
- **Create widget** button
- **Get code** button
- Configuration management buttons:
  - Copy icon
  - Reset button
  - Save button

### 4.3 Search Options (Collapsible Section)
**Description**: "Queries, filtering and suggestions"

**Configuration Toggles**:

1. **Use search results** (Enabled by default)
   - Description: "If disabled, the search results will not be used to generate the answer. The LLM will generate responses based solely on its own knowledge base."

2. **Show button to filter**
   - Description: "The user will be able to apply filters to the search"

3. **Preselected filters** (Disabled by default)
   - Description: "Define filters that will be applied by default to any query"

4. **Rephrase query** (Enabled by default)
   - Description: "Rephrase the query in order to obtain better semantic search results. (Note: the query is unchanged regarding keyword search)"

5. **Custom rephrase prompt**
   - Description: "This prompt will be used to rephrase the user query to optimize search results. Note: the original user query will be preserved when passing it to the LLM to obtain an answer"

6. **Semantic re-ranking** (Enabled by default)
   - Description: "Initially retrieve a larger amount of semantic and keyword search results and then filter them down to the most contextually accurate matches using a re-ranking model. The result is a more accurate and better ordered list of results"

7. **Reciprocal Rank Fusion**
   - Description: "Combine keyword and semantic search results using Reciprocal Rank Fusion (RRF) algorithm"

### 4.4 Generative Answer and RAG (Collapsible Section)
**Description**: "Options to produce the answer"

**Configuration Options**:

1. **Generate answer** (Enabled by default)
   - Description: "Generate a response to the user's questions and allow to chat with the system. If you disabled this toggle the user will only get keywords and vector search results"

2. **Specific generative model**
   - Dropdown: "Current generative model"
   - Description: "Use a different generative model than the default one defined in the Knowledge Box AI settings"

3. **Use a specific prompt** (Disabled by default)
   - Description: "It will take precedence over the user prompt defined in the Knowledge Box settings. Note: the prompt strategy might change depending on the generative model"

4. **Use a specific system prompt** (Disabled by default)
   - Description: "It will take precedence over the system prompt defined in the Knowledge Box settings. Note: the prompt strategy might change depending on the generative model"

5. **Ask a specific resource** (Disabled by default)
   - Description: "The generative answer will use the entire text of this specific resource as context instead of the search results matching the query. Note: If the resource text content is too long, it might be shortened when passed to the LLM"

6. **Show reasoning**
   - Description: "Display the reasoning steps (if provided by the generative model)"

7. **Limit token consumption** (Disabled by default)
   - Description: "Defines the maximum number of tokens that the model will read and/or generate. A low number may prevent your users to see results"

8. **Prefer markdown format**
   - Description: "Generate the answer using markdown format"

### 4.5 Result Display Options (Collapsible Section)
**Description**: "Choose what to show in the results"

**Display Configuration**:

1. **Display results** (Enabled by default)
   - Description: "Search results are displayed under the answer"
   - **Display modes** (Radio buttons):
     - **Show all results from the search**
     - **Show citations** (Selected by default)
       - Description: "Paragraphs identified as citations based on similarity"
     - **Show LLM citations - Beta**
       - Description: "Paragraphs identified as citations by an LLM"

2. **Customize citation threshold**
   - Description: "Specifies the minimum similarity score required to assign a citation to the generated answer. Note: high threshold may result in no citations being shown, and low threshold may result in improper citations"

3. **Display metadata**
   - Description: "Metadata are displayed under the title of each resource"

4. **Display thumbnails** (Enabled by default)
   - Description: "Thumbnails are a preview of the resource"

5. **Limit resources display to top_k**
   - Description: "The initial result list will be compliant with the maximum paragraphs amount defined by the top_k parameter, and by scrolling down, no extra results will be loaded"

6. **Show attached images**
   - Description: "Show images associated with the paragraphs retrieved in the results"

7. **Display field list** (Disabled by default)
   - Description: "Display a section listing all the fields of the resource in the right sidebar of the viewer. This section is only visible for resources containing multiple fields"

8. **Relations**
   - Description: "Display relations between resources"

### 4.6 User-Intent Routing (Collapsible Section)
**Description**: "Dynamically apply a given search configuration"

**Configuration**:
1. **Use routing**
   - Description: "Provide prompts that will allow the model to define the search configuration to apply depending on the user intent detected in the question"

---

## 5. WIDGETS

### 5.1 Widgets List Page (Empty State)
**Display**: "Widgets - Create and manage search widgets to embed in your projects"
**Message**: "You haven't created any widget yet. You can create them from the search page based on a search configuration or by clicking on 'Create Widget'."
**Action**: "Create widget" button (black, top right)

### 5.2 Create Widget Modal
**Title**: "Create a widget"
**Description**: "The widget code can be embedded directly on your site, it will be saved in the widgets section"

**Form Fields**:
1. **Name** - Text input for widget name

**Actions**:
- "Cancel" button
- "Set up widget" button (blue)

### 5.3 Widget Configuration Page
**Title**: "My Widget"
**Description**: "The embedded widget will reflect the search configuration and widget options set below"

**Top Actions**:
- "Configure" button
- "Embed widget" button (black with code icon)
- Three-dot menu

**Left Panel - Widget Options** (Collapsible sections):

#### Widget Appearance
1. **Customize search bar placeholder**
   - Description: "Change the text that you want to appear in the search bar when not active. The default text is 'Type your question here'"

2. **Customize chat bar placeholder**
   - Description: "Change the text that you want to appear in the chat bar when not active. The default text is 'Let's talk'"

3. **Customize insufficient data message**
   - Description: "Customize the message displayed when there's insufficient data to generate a response to the user's question. This field allows HTML markup"

#### Widget Style (Radio buttons)
1. **Embedded in page** (Selected)
2. **Chat mode**
3. **Popup modal style (opens above your content)**

#### Widget Theme (Radio buttons)
1. **Light theme** (Selected)
2. **Dark theme**

4. **No chat history**
   - Description: "The previous questions and answers in the chat mode are not passed as context when generating a new answer"

5. **Persist chat history**
   - Description: "The chat history will be stored in the browser localstorage and restored whenever the widget is reopened. Note: if disabled, the history is lost whenever the widget is closed"

#### Feedback Options (Radio buttons)
1. **No feedback**
2. **Global feedback on the generated answer** (Selected)
3. **Detailed feedback on answer and search results**

6. **Copy button disclaimer**
   - Description: "Display a message when the user use the Copy button to copy the answer"

7. **Collapse text blocks by default**
   - Description: "Text blocks from citations and results will be initially collapsed"

8. **Customize citation visibility**
   - Description: "Choose whether citations are expanded or collapsed initially"

#### Search Configuration
- Dropdown selector: "Standard answer generation - Agentic RAG"
- Sub-sections expandable:
  - **Search options** - "Queries, filtering and suggestions"
  - **Generative answer and RAG** - "Options to produce the answer"
  - **Result display options** - "Choose what to show in the results"
  - **User-intent routing** - "Dynamically apply a given search configuration"

**Right Panel - Test the Widget**:
- Live preview of widget
- Search input: "Type your question here"

---

## 6. SETTINGS (KNOWLEDGE BOX CONFIGURATION)

### 6.1 Knowledge Box Settings
**Configuration Fields**:

1. **ID** (Read-only)
   - Example: `1eeffe49-7e69-4807-9a30-df883a0b9de4`

2. **Zone** (Read-only)
   - Example: `europe-1`

3. **Slug**
   - Example: `first-box`
   - Description: URL-friendly identifier

4. **Name**
   - Example: `First Box`
   - Description: Display name for the Knowledge Box

5. **Description**
   - Multi-line text area
   - For documentation purposes

6. **Enable hidden resources** (Toggle)
   - Hide/show resources from search

7. **Allowed origins (CORS)**
   - Multi-line text area
   - One domain per line
   - For widget embedding security

8. **Allowed IP addresses**
   - Multi-line text area
   - One IP address per line
   - With info icon for help

9. **Status: private**
   - Visibility toggle
   - "Publish" button to make public

**Actions**:
- "Save" button
- "Cancel" button

---

## 7. AI MODELS CONFIGURATION

### 7.1 Model Selection Interface
**Tabs**:
1. **Answer generation** (Active)
2. **On-demand summarization** (Green dot - active/configured)
3. **Embeddings model** (Green dot - active/configured)
4. **Extract & split** (Not shown in detail)
5. **Anonymization** (Green dot - active/configured)

### 7.2 Answer Generation Models
**Title**: "Select the LLM used to generate answers"
**Description**: "Choose the LLM that will work the best to answer your user's questions"

**Available Models** (Radio button selection):

#### OpenAI Models:
- Do not generate answers
- **OpenAI + Azure ChatGPT-4o** (Selected)
- OpenAI + Azure ChatGPT-4o-mini
- OpenAI + Azure ChatGPT-o1
- OpenAI + Azure ChatGPT-o1-mini
- OpenAI + Azure ChatGPT-o3
- OpenAI + Azure ChatGPT-o3-mini
- OpenAI + Azure ChatGPT-5
- OpenAI + Azure ChatGPT-5 Mini
- OpenAI + Azure ChatGPT-5 Chat
- OpenAI + Azure ChatGPT-5 Nano

#### Anthropic Models:
- Anthropic Claude 3 Opus
- Anthropic Claude 4 Opus
- Anthropic Claude 4 Sonnet
- Anthropic Claude 4.5 Sonnet
- Anthropic Claude 4.5 Haiku
- Anthropic Claude 3.5 Haiku

#### Google Models:
- Google Gemini Flash 2.0 Lite
- Google Gemini Flash 2.0
- Google Gemini Pro 2.5
- Google Gemini Flash 2.5
- Google Gemini Flash 2.5 Lite

#### OpenAI (non-Azure):
- OpenAI ChatGPT-4
- OpenAI ChatGPT-4o
- OpenAI ChatGPT-4o-mini

**Action Buttons**:
- "Previous" button
- "Save settings" button (blue)

### 7.3 Premium Features Upsell Modal
**Trigger**: Clicking on premium features (Agents, Activity log, NER, RAG lab)

**Modal Title**: "Upgrade your plan to unlock '[Feature Name]'"
**Description**: "This feature is not included in your plan, upgrade now to unlock the full potential of Agentic RAG"

**Premium Features Listed**:
1. **LLMs availability**
   - "Use Anthropic, Google and Mistral LLMs"

2. **Prompt**
   - "Set a custom prompt"

3. **Summary**
   - "Summarize resources on-demand"

4. **Synonyms**
   - "Manage a list of synonyms"

5. **Task automation**
   - "Labels, summary, Q&A, global questions..."

6. **Activity log**
   - "Follow your Knowledge Box's activity"

7. **Indexed data size**
   - "Larger database size per Knowledge Box"

**Actions**:
- "Cancel" button
- "Explore plans" button (blue)

---

## 8. METRICS & EVALUATION

### 8.1 RAG Evaluation Metrics (REMI) Page
**Title**: "RAG Evaluation Metrics (REMI)"
**Description**: "Evaluate the performances of your RAG pipeline with REMI. The evaluation is performed over all the queries submitted by your users to your Knowledge Box"

**Time Period Selector**:
- Dropdown: "Show metrics on the last 7 days"

### 8.2 Health Status Section
**Title**: "Health status"
**Description**: "See how healthy your RAG pipeline is by checking the average, min and max values of each metric over the selected period of time"

**Empty State Message**:
"There is no available data on the selected period or the scores for the data are not computed yet"

**Metrics Definitions**:
1. **Answer relevance**
   - "Relevance of the generated answer to the user query"

2. **Context relevance**
   - "Relevance of the retrieved context to the user query"

3. **Groundedness**
   - "Measures the extent to which the response is based on the context"

### 8.3 Performance Evolution Section
**Title**: "Performance evolution"
**Description**: "Track the evolution of your RAG pipeline performances"

**Empty State**:
"There is no available data on the selected period or the scores for the data are not computed yet"

### 8.4 Download Metrics
**Feature**: "You can download detailed metrics for each question in the Activity log"
- Link to Activity log (blue hyperlink)

### 8.5 Missing Knowledge Section
**Title**: "Missing knowledge in my Knowledge Box"
**Description**: "Find out what knowledge is missing in your Knowledge Box by reviewing the questions for which there was no answer or a bad context relevance score"

**Expandable Categories** (All showing "No data is matching your criteria"):
1. **Questions without answer in 2025-11**
   - Page 1

2. **Max context relevance score lower than 60% in 2025-11**
   - Page 1

3. **Answers with negative feedback in 2025-11**
   - Page 1

---

## 9. API KEYS MANAGEMENT

### 9.1 API Keys Page
**Title**: "API keys"
**Warning**: "Important! Copy and save the API key. It will disappear once added"

**Create New API Key**:
- **Name** field - Text input
- **Role** dropdown (expanded):
  - Reader
  - Writer
  - Manager
- **Add** button

**Instructions**:
- Info icon with "How to use your API key" (expandable)

---

## 10. LABEL SETS

### 10.1 Label Sets Page
**Title**: "Label sets"
**Description**: "Browse and edit label sets"

**Empty State**:
"This is where you will see your labels when you create a label set"

**Additional Info**:
"If you used CLI/API to create labels, you should declare them in the Dashboard"

**Actions**:
- "Add new" button (blue with + icon)
- "Check missing labels" button

### 10.2 Create Label Set Modal
**Fields**:

1. **Label set name**
   - Text input

2. **Apply labels to:** (Radio buttons)
   - **Resources**
   - **Text blocks**

**Warning Message** (Yellow banner):
"This property cannot be modified afterward"

3. **Exclusive label** (Checkbox)
   - "Add only one label by label set"
   - Description: "By default, a resource / text block can have several labels from a same label set"

4. **Labels list**
   - Text area with add/delete icons
   - Description: "List the labels comma-separated and press Enter to add them"

**Actions**:
- "Save" button (disabled in screenshot)
- "Cancel" button

---

## 11. USERS MANAGEMENT

### 11.1 Users Page
**Title**: "Users"

**Add New User Section**:
- **User's email** - Text input
- **Role** dropdown (showing "Reader")
- **Add** button (disabled)

**Users List**:
**Headers**:
- Name
- Email
- Role

**Sort Option**:
- "Sort by role" dropdown (top right)

**Example User**:
- Name: Umoh Bassey-Duke
- Email: umoh@sku.africa
- Role: Manager

---

## 12. PREMIUM FEATURES (Locked)

The following features show upgrade prompts:

### 12.1 Agents
- Requires plan upgrade
- Part of premium tier

### 12.2 Activity Log
**Locked Feature Benefits**:
- Follow your Knowledge Box's activity
- Download detailed query metrics

### 12.3 NER (Named Entity Recognition)
- Advanced text processing
- Requires premium plan

### 12.4 RAG Lab
- Experimental RAG features
- Premium feature

---

## 13. BRANDING & UI ELEMENTS

### 13.1 Visual Design
- **Primary Color**: Blue (#2563EB or similar)
- **Secondary Color**: Green (for Progress branding)
- **Background**: Light gray/white
- **Sidebar**: Light blue-gray (#F1F5F9 or similar)

### 13.2 Icons
- Modern, minimalist icon set
- Consistent sizing and spacing
- Mix of outlined and filled icons

### 13.3 Typography
- Clean, modern sans-serif font
- Clear hierarchy (headings, body, labels)
- Good contrast for readability

### 13.4 Interaction Patterns
- Toggle switches for boolean options
- Radio buttons for exclusive selections
- Collapsible sections with chevron indicators
- Modal overlays for focused tasks
- Inline forms with immediate feedback

---

## 14. KEY DIFFERENTIATORS

### 14.1 Search Configuration Flexibility
- Granular control over search behavior
- RAG vs. pure LLM toggle
- Multiple ranking algorithms
- Query rephrasing options

### 14.2 Multi-Model Support
- OpenAI (Azure and direct)
- Anthropic Claude family
- Google Gemini family
- Model switching per configuration

### 14.3 Widget Customization
- Three display modes (embedded, chat, popup)
- Light/dark themes
- Customizable placeholders
- Feedback collection options
- Citation visibility controls

### 14.4 Enterprise Features
- CORS configuration
- IP whitelisting
- Role-based access control (Reader, Writer, Manager)
- API key management
- User management

### 14.5 Data Synchronization
- Multiple source types
- Desktop sync agent
- Remote server deployment
- Automatic updates from external sources

### 14.6 Evaluation & Monitoring
- RAG-specific metrics (REMI)
- Answer relevance tracking
- Context relevance scoring
- Groundedness measurement
- Knowledge gap identification

---

## 15. FEATURE PRIORITY MATRIX

### P0 (Critical - Core RAG Functionality):
1. Document upload (File, Folder, Links, Text)
2. Basic search with RAG
3. LLM integration (at least one provider)
4. Embedding generation
5. Vector storage and retrieval
6. Resource list/management
7. Basic configuration

### P1 (High - Essential User Experience):
1. Search configuration panel
2. Widget creation and embedding
3. Multiple upload types (Sitemap, Q&A)
4. Model selection interface
5. Citation display
6. Knowledge Box settings
7. API key management

### P2 (Medium - Advanced Features):
1. Synchronization (Folder, RSS, Sitemap)
2. Query rephrasing
3. Semantic re-ranking
4. Custom prompts
5. Result display customization
6. User management
7. Label sets

### P3 (Nice to Have - Premium/Advanced):
1. RAG evaluation metrics (REMI)
2. Activity logging
3. Agents system
4. NER capabilities
5. RAG lab experiments
6. Advanced analytics
7. On-demand summarization

---

## Next Steps

The following companion documents should be created:
1. **UI/UX Specifications** - Detailed wireframes and component specs
2. **Data Models** - Database schema and entity relationships
3. **API Specifications** - REST API endpoints and payloads
4. **Implementation Roadmap** - Phased development plan
5. **Technical Architecture** - System design and component interactions
