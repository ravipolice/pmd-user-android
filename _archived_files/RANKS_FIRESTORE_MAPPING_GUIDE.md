# Ranks Firestore Mapping Guide

## Excel to Firestore Mapping

Based on your Excel structure, here's how to map each row to Firestore Rank documents:

### Column Mapping

| Excel Column | Firestore Field | Notes |
|-------------|----------------|-------|
| `rank_id` | `rankId` (optional) | Internal identifier |
| `rank_label` | `rankLabel` (optional) | Full rank name for reference |
| `category` | `category` (optional) | BOTH, DISTRICT, or COMMISSIONERATE |
| `equivalent_rank` | `name` | **Primary field** - Use this as the rank abbreviation |
| `seniority_order` | `displayOrder` | Use this number for sorting (1 = highest, 13 = lowest) |
| `aliases` | `aliases` (optional array) | Split comma-separated values into array |
| `active` | `isActive` | Convert TRUE → true, FALSE → false |
| `remarks` | `remarks` (optional) | Additional notes |

### Important: Multiple Documents per Row

Since your Excel has one row per rank category but your system uses individual abbreviations (APC, CPC, WPC, etc.), you need to create **separate Firestore documents for each alias** you want to support.

### Example: Police Constable Row

**Excel Row:**
- rank_id: `PC`
- rank_label: `Police Constable`
- equivalent_rank: `PC`
- seniority_order: `13`
- aliases: `APC, CPC, WPC, PCW, SPC`
- active: `TRUE`
- remarks: `Senior constabulary`

**Firestore Documents to Create:**

1. **Document 1:**
```json
{
  "name": "APC",
  "rankLabel": "Police Constable",
  "rankId": "PC",
  "displayOrder": 13,
  "requiresMetalNumber": true,
  "isActive": true,
  "category": "BOTH",
  "aliases": ["APC", "CPC", "WPC", "PCW", "SPC"],
  "remarks": "Senior constabulary"
}
```

2. **Document 2:**
```json
{
  "name": "CPC",
  "rankLabel": "Police Constable",
  "rankId": "PC",
  "displayOrder": 13,
  "requiresMetalNumber": true,
  "isActive": true,
  "category": "BOTH",
  "aliases": ["APC", "CPC", "WPC", "PCW", "SPC"],
  "remarks": "Senior constabulary"
}
```

3. **Document 3:**
```json
{
  "name": "WPC",
  "rankLabel": "Police Constable",
  "rankId": "PC",
  "displayOrder": 13,
  "requiresMetalNumber": true,
  "isActive": true,
  "category": "BOTH",
  "aliases": ["APC", "CPC", "WPC", "PCW", "SPC"],
  "remarks": "Senior constabulary"
}
```

4. **Document 4:**
```json
{
  "name": "PCW",
  "rankLabel": "Police Constable",
  "rankId": "PC",
  "displayOrder": 13,
  "requiresMetalNumber": false,  // Note: PCW might not require metal number
  "isActive": true,
  "category": "BOTH",
  "aliases": ["APC", "CPC", "WPC", "PCW", "SPC"],
  "remarks": "Senior constabulary"
}
```

5. **Document 5:**
```json
{
  "name": "PC",
  "rankLabel": "Police Constable",
  "rankId": "PC",
  "displayOrder": 13,
  "requiresMetalNumber": true,
  "isActive": true,
  "category": "BOTH",
  "aliases": ["APC", "CPC", "WPC", "PCW", "SPC"],
  "remarks": "Senior constabulary"
}
```

### Ranks Requiring Metal Number

Based on your current constants, these ranks require metal numbers:
- **APC, CPC, WPC, PC** (from Police Constable)
- **AHC, CHC, WHC, HC** (from Head Constable)

For all other ranks, set `requiresMetalNumber: false`.

### Quick Reference: Which Ranks Need Metal Numbers?

From your Excel aliases, check if any of these are in the aliases column:
- APC, CPC, WPC, PC → `requiresMetalNumber: true`
- AHC, CHC, WHC, HC → `requiresMetalNumber: true`
- All others → `requiresMetalNumber: false`

### Simplified Approach (Recommended)

If you want to simplify, you can create documents using just the `equivalent_rank` as the `name` field, and ignore the aliases. This would create one document per Excel row:

**Example for "Police Constable":**
```json
{
  "name": "PC",  // From equivalent_rank column
  "rankLabel": "Police Constable",
  "displayOrder": 13,
  "requiresMetalNumber": true,
  "isActive": true
}
```

But if your system currently uses "APC", "CPC", "WPC" as separate ranks, you'll need separate documents for each.








