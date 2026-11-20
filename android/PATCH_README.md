# Frontend-only patch — HSC Academic Solution Android client

## Root causes found
- `HomeFragment` initialized the network status and logout button only. It never connected `CatalogViewModel`, `SubjectAdapter`, or `subjectList` to the subjects endpoint. As a result, the Home screen had no code path to render fetched subjects.
- Login input colors were hard-coded to dark text, and the shared text-input styling did not explicitly set a password visibility icon tint. These choices can cause poor contrast or hidden controls in theme variations.
- Paper/Chapter/Question screens did not consistently reset retry visibility after successful loads; Question retry did not reset pagination.
- Content repository returned successful-but-empty HTTP bodies without a clear user-facing error.

## Files included
Only changed/new files are included. Copy these paths into the Android project, preserving directories:
- `app/src/main/java/bd/hscsolution/student/ui/HomeFragment.java`
- `app/src/main/java/bd/hscsolution/student/ui/QuestionListFragment.java`
- `app/src/main/java/bd/hscsolution/student/ui/PaperFragment.java`
- `app/src/main/java/bd/hscsolution/student/ui/ChapterFragment.java`
- `app/src/main/java/bd/hscsolution/student/content/ContentRepository.java`
- `app/src/main/res/layout/fragment_login.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-night/colors.xml` (new)

## Changes
- Home now fetches subjects through the existing repository/ViewModel, renders via RecyclerView/SubjectAdapter, navigates to the paper list, and handles loading, empty, error, and retry states.
- Error/empty/retry visibility is made consistent in catalog screens.
- Question filter submission resets pagination; retry also returns to page 1.
- Question pagination remains page size 20, with previous/next state derived from API response.
- Empty questions state remains the student-facing published-only message.
- Successful API responses with missing bodies now fail with a clear content-loading error.
- Login text and hints now use theme colors; password toggle icon tint is explicitly styled.
- Night palette resources provide dark-mode contrast while preserving the existing layout resource references.
- No backend files are included or changed.

## Tests / verification
- XML resources parse successfully.
- Source review confirms the Home screen now observes catalog state and invokes the subjects endpoint.
- Full Android Gradle build could not be run in this environment: the Gradle wrapper attempted to download Gradle 8.13, but network access to `services.gradle.org` was unavailable.
- Backend-dependent scenarios (subject/paper/chapter/question data, published-only enforcement, filters, and pagination) still require running the app against the user's backend. The frontend passes the selected filters and pagination parameters to the existing API; published-only filtering is server-contract behavior and is not reimplemented client-side.
