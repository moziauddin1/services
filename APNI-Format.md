APNI Format
===========

The APNI Format is the output developed over many years by the APNI team including Greg Whitbread. It is a bibliographic
output of Scientific plant names along with where they have been published and the relationship between the uses of the
 names in different publications.


Example output in text format
-----------------------------
 
Doodia R.Br.
Brown, R. (1810), Prodromus Florae Novae Hollandiae: 151 [tax. nov.]

    Type: (not designated).

Smith, J. (1875), Historia Filicum: 309

    Lectotype: Doodia aspera R.Br.

Mueller, F.J.H. von (1882), Systematic Census of Australian Plants: 138

taxonomic synonym of: Woodwardia Sm.

Bailey, F.M. (1913), Comprehensive Catalogue of Queensland Plants: 644

Beadle, N.C.W., Evans, O.D. & Carolin, R.C. (1962), Handbook of the Vascular Plants of the Sydney District and Blue Mountains: 78-79

Kramer, K.U. in Kubitzki, K. (ed.) (1990), Blechnaceae. The Families and Genera of Vascular Plants 1: 63, Fig. 21B, C

Wilson, P.G. in Harden, G.J. (ed.) (1990), Blechnaceae. Flora of New South Wales 1: 67-68

common name: Rasp Ferns

Entwisle, T.J. in Walsh, N.G. & Entwisle, T.J. (ed.) (1994), Ferns and Allied Plants. Flora of Victoria Edn. 1, 2: 102

Green, P.S. in Wilson, A.J.G. (ed.) (1994), Norfolk Island & Lord Howe Island. Flora of Australia 49: 613

Parris, B.S. in McCarthy, P.M. (ed.) (1998), Doodia. Flora of Australia 48: 385 APC

    APC Comment: Hybridisation between species occurs relatively easily, and is possibly under-reported in natural populations (Parris, 1998).
    APC Dist.: NT, SA, Qld, NSW, LHI, NI, ACT, Vic, Tas

Harden, G.J. & Murray, L.J. in Harden, G.J. & Murray, L.J. (ed.) (2000), Flora of New South Wales Supplement 1: 10

Wilson, Peter G. in Harden, G.J. (ed.) (2000), Blechnaceae. Flora of New South Wales Revised Edition 1: 67-68

common name: Rasp Ferns

Smith, A.R., Pryer, K.M., Schuettpelz, E., Korall, P., Schneider, H. & Wolf, P.G. (2006), A classification for extant ferns. Taxon 55(3): 716

Mabberley, D.J. (2008), Mabberley's Plant-Book Edn. 3: 286

Christenhusz, M.J.M., Zhang, X.C. & Schneider, H. (2011), A linear sequence of extant families and genera of lycophytes and ferns. Phytotaxa 19: 16

taxonomic synonym of: Blechnum L.


# Sorting the output

The sorting of the output of references and instances (or usages of the name and relationships) after the name is broken
into two main parts:

 * References and,
 * Instances

Instances for a name are grouped by reference.
The references are sorted, then the instances are sorted within a reference.
 
The two parts are interconnected by the type of name use in the reference, for example the reference containing the first
correct publication of a name [tax. nov.] is ordered before alphabetical ordering, and we call it the "Protologue" or 
"Primary Instance". The primary instance may be in a reference published after the first known publication of the name.

The sorts are nested, so if the first sort key is equal the two items are sorted by the next sort key and so on.

In general the Reference is displayed followed by a sorted list of Instances (name usages). The reference is 
redisplayed within the list of instances if:

 * the page number changes
 * the cited by instance changes

## Algorithm

    Get all instances for a name
    Group instances by reference
    Sort the references
    For each Reference
      display the reference citation, page number, APC status
      sort instances within the reference
      for each Instance in this reference
        if the page number or cited by changed display the reference citation, page number, APC status
        display the relationship and other name
        display type information or instance notes
      end for instances
    end for reference

## Reference sort order

There are some data limitations to the sorting of references. Some references do not have Integer Year values (i.e. null
year), and they are displayed as a lower value year. The pages of the usage are in text and may contain information about
figures, so the sorting on those is a text sort which means 1s come before 2s etc. (so it may sort 1, 11, 2...)

We sort the references into a reference instance map for use in the display. The references are sorted by:

 1. reference year (null, 1893, 1900, 1901, 1902...)
 2. reference containing a Protologue instance
 3. reference containing a Primary instance
 4. Alphabetically by reference citation A -> Z
 5. Reference page order (String order sort, 1s first then 2s....)

The effect of this sort is that if the reference has an earlier year (or null year) it will be displayed first even if it
doesn't contain the Protologue/Primary instance, but if two references have the same year (e.g. null, null) the primary 
instance reference will come first.

## Instance sort order

Instances **within** references are sorted and grouped for display. Instances are sorted by:

If cited by instances are different then sort using Cited By instances

Instances **without** a cited by instance are first (stand alone instances)

Stand alone instances are sorted by:

 1. Instance type sort order (database field)
 1. Cites -> reference -> year or the year of the cited reference
 1. Reference citation alphabetical order A -> Z
 1. Instance page number (null,1,2,3,4...)

Then relationship instances are displayed e.g. synonym of, missapplied to, are sorted by:

  1. The cited by reference year (null, 1893, 1900, 1901, 1902...)
  1. The cited by reference citation alphabetical order A -> Z
  1. The cited by instance page number (null,1,2,3,4...)
  1. The cited by instance id

## Instance display

If this is the first instance in this reference or the page changes or the cited by instance changes the referecne 
citation is displayed followed by the page number (protologue pdf if in the old system) and the APC tick if it is the 
APC instance.

Instance "type" notes ['Type', 'Lectotype', 'Neotype'] are then displayed if present

Instances are displayed based on the instance type. Specifically instances are displayed differently if:

 * a synonym or unsourced
 * it has instances for cited by: has Synonym, or missapplication
 * other synonyms for not cited by: not synonym or missapplication
 * if this instance is a missaplication: misapplied to...

Then a list of displayable, not Type, instance notes are displayed ( not ['Type', 'Lectotype', 'Neotype', 'EPBC Advice',
 'EPBC Impact', 'Synonym']).
 
## instance type sort order

 1. 5,   orthographic variant
 1. 10,  replaced synonym
 1. 10,  basionym
 1. 20,  pro parte replaced synonym
 1. 30,  nomenclatural synonym
 1. 40,  doubtful nomenclatural synonym
 1. 50,  pro parte nomenclatural synonym
 1. 70,  pro parte misapplied
 1. 80,  doubtful misapplied
 1. 90,  doubtful pro parte misapplied
 1. 100, taxonomic synonym
 1. 110, pro parte taxonomic synonym
 1. 120, doubtful taxonomic synonym
 1. 130, doubtful pro parte taxonomic synonym
 1. 140, synonym
 1. 150, pro parte synonym
 1. 160, doubtful synonym
 1. 170, doubtful pro parte synonym
 1. 400, autonym
 1. 400, [default]
 1. 400, comb. et stat. nov.
 1. 400, homonym
 1. 400, invalid publication
 1. 400, sens. lat.
 1. 400, common name
 1. 400, vernacular name
 1. 400, comb. nov.
 1. 400, nom. et stat. nov.
 1. 400, nom. nov.
 1. 400, tax. nov.
 1. 400, primary reference
 1. 400, [n/a]
 1. 400, implicit autonym
 1. 400, [unknown]
 1. 400, secondary reference
 1. 400, isonym
 1. 400, trade name
 1. 400, misapplied
 1. 400, excluded name
 1. 400, doubtful invalid publication
