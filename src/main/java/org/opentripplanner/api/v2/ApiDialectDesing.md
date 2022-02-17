

# GraphQL Schema Dialect Design

We will use a `csv` file to maintain the GTFS and Transmodel dialects for the main GraphQL API. 
 - A CSV file is easy to maintain
 - It can be used for other purposes, like generating documentation
 - Keeping both the GTFS and Transmodel dialects in one file will help us maintain both dialects, 
   keep it up to date, and enforce consistency.
 - We uses a `+` prefix for tokens we want to translate in free text documentation. Hopefully this
   does not clash with html and markdown, witch is legal to use in documentation. We use the prefix 
   to be able to verify all tokens are translated, the translated schema should not contain 
  `+<Identifier>` (no white-space  between the `+` and the `<identifier>`),  but `+ <anything>` is 
  ok. If needed we should use `:` to support nested scopes like `+Character:name` or 
  `+Query:hero:episode`.


## Example

### Given a GraphQL Schema
 
```graphql
"We use ISO 8601 for the +Date type."
scalar Date
"Each +Character may appair in multiple +Episodes"
enum Episode { NEWHOPE, EMPIRE, JEDI }
type Character { name: String! appearsIn: [Episode]! }
type Query {
  hero(episode: Episode): Character @deprecated(reason: "Use *+Character:name* instead.")
}
```

### With the following translation

> TODO : Can we remove the GQLType in the table

| GQLType | Element | Attribute | Parameter | GTFS | Transmodel |
| ------- | ------- | --------- | --------- | ---- | ---------- |
| `scalar` | Date | | | DateTime | LocalDate |
| `enum` | Episode | | | Show | NamedSequenceInstanceOfDistributableUnit |
| `enum` | Episode | NEWHOPE |  | HOPE | HOPE_IT_IS |
| `enum` | Episode | EMPIRE |  | THE_STATES | THE_UNION |
| `enum` | Episode | JEDI |  | HERO | NINJA_WOMAN |
| `type` | Character |  |  | Star | Artist |
| `type` | Character | name |  | title | firstAndLastName |
| `type` | Character | appearsIn | | staringIn | partOf |
| `type` | Query | hero | episode | show | sequenceName |
| `text` | Episodes | | | Shows | NamedSequenceInstancesOfDistributableUnit |

### We get the following result


#### GTFS GraphQL Schema

```graphql
"We use ISO 8601 for the +DateTime type."
scalar DateTime
"Each Character may appair in multiple Show"
enum ShowNumber { HOPE, THE_STATES, HERO }
type Star { title: String! staringIn: [Show]! }
type Query {
  hero(show: Show): Star @deprecated(reason: "Use *title* instead.")
}
```

#### Transmodel Schema


```graphql
"We use ISO 8601 for the LocalDate type."
scalar LocalDate
"Each Artist may appair in multiple NamedSequenceInstancesOfDistributableUnit"
enum Episode { HOPE_IT_IS, THE_UNION, NINJA_WOMAN }
type Artist { firstAndLastName: String! partOf: [NamedSequenceInstanceOfDistributableUnit]! }
type Query {
  hero(sequenceName: NamedSequenceInstanceOfDistributableUnit): Artist @deprecated(reason: "Use *firstAndLastName* instead.")
}
```
